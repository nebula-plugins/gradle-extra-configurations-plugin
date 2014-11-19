/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nebula.plugin.extraconfigurations

import nebula.test.IntegrationSpec
import spock.lang.Ignore

class ProvidedBasePluginIntegrationTest extends IntegrationSpec {
    def "Can compile production code dependent on dependency declared as provided"() {
        when:
        buildFile << """
apply plugin: 'java'
apply plugin: 'provided-base'

repositories {
    mavenCentral()
}

dependencies {
    provided 'org.apache.commons:commons-lang3:3.3.2'
}
"""
        createProductionJavaSourceFile()
        runTasksSuccessfully('compileJava')

        then:
        new File(projectDir, 'build/classes/main/nebula/extraconf/HelloWorld.class').exists()
    }

    def "Can test production code dependent on dependency declared as provided"() {
        when:
        buildFile << """
apply plugin: 'java'
apply plugin: 'provided-base'

repositories {
    mavenCentral()
}

dependencies {
    provided 'org.apache.commons:commons-lang3:3.3.2'
    testCompile 'junit:junit:4.8.2'
}
"""
        createProductionJavaSourceFile()
        createTestSourceJavaClass()
        runTasksSuccessfully('test')

        then:
        new File(projectDir, 'build/classes/main/nebula/extraconf/HelloWorld.class').exists()
        new File(projectDir, 'build/classes/test/nebula/extraconf/HelloWorldTest.class').exists()
        new File(projectDir, 'build/test-results/TEST-nebula.extraconf.HelloWorldTest.xml').exists()
    }

    private void createProductionJavaSourceFile() {
        File javaFile = createFile("src/main/java/nebula/extraconf/HelloWorld.java")
        javaFile << """
package nebula.extraconf;

import org.apache.commons.lang3.StringUtils;

public class HelloWorld {
    public String getMessage() {
        return StringUtils.upperCase("Hello World!");
    }
}
"""
    }

    private void createTestSourceJavaClass() {
        File javaTestFile = createFile('src/test/java/nebula/extraconf/HelloWorldTest.java')
        javaTestFile << """
package nebula.extraconf;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class HelloWorldTest {
    @Test
    public void getMessage() {
        assertEquals("HELLO WORLD!", new HelloWorld().getMessage());
    }
}
"""
    }

    def "Adds provided dependencies to PROVIDED scope if Idea plugin is applied"() {
        when:
        buildFile << """
apply plugin: 'java'
apply plugin: 'provided-base'
apply plugin: 'idea'

repositories {
    mavenCentral()
}

dependencies {
    provided 'org.apache.commons:commons-lang3:3.3.2'
}
"""
        runTasksSuccessfully('idea')

        then:
        File ideaModuleFile = new File(projectDir, "${moduleName}.iml")
        ideaModuleFile.exists()
        def moduleXml = new XmlSlurper().parseText(ideaModuleFile.text)
        def orderEntries = moduleXml.component.orderEntry.findAll { it.@type.text() == 'module-library' && it.@scope.text() == 'PROVIDED' }
        orderEntries.find { it.library.CLASSES.root.@url.text().contains('commons-lang3-3.3.2.jar') }
    }

    def 'verify eclipse add provided'() {
        buildFile << '''
            apply plugin: 'java'
            apply plugin: 'provided-base'
            apply plugin: 'eclipse'

            repositories { mavenCentral() }

            dependencies {
                provided 'org.apache.commons:commons-lang3:3.3.2'
            }
        '''.stripIndent()

        when:
        runTasksSuccessfully('eclipse')

        then:
        File eclipseClasspath = new File(projectDir, '.classpath')
        eclipseClasspath.exists()
        def classpathXml = new XmlSlurper().parseText(eclipseClasspath.text)
        classpathXml.classpath.classpathentry.find { it?.@path?.contains 'org.apache.commons/commons-lang3/3.3.2' } != null
    }

    def "Publishing provided dependencies to a Maven repository preserves the scope"() {
        given:
        File repoUrl = new File(projectDir, 'build/repo')

        when:
        buildFile << """
apply plugin: 'java'
apply plugin: 'provided-base'
apply plugin: 'maven-publish'

group = 'nebula.extraconf'
version '1.0'

repositories {
    mavenCentral()
}

dependencies {
    provided 'org.apache.commons:commons-lang3:3.3.2'
}

publishing {
    publications {
        mavenJava(MavenPublication) {
            from components.java
        }
    }

    repositories {
        maven {
            url '$repoUrl.canonicalPath'
        }
    }
}
"""
        runTasksSuccessfully('publish')

        then:
        File pomFile = new File(repoUrl, "nebula/extraconf/$moduleName/1.0/$moduleName-1.0.pom")
        pomFile.exists()
        def pomXml = new XmlSlurper().parseText(pomFile.text)
        def dependencies = pomXml.dependencies
        dependencies.size() == 1
        def commonsLang = dependencies.dependency[0]
        commonsLang.groupId.text() == 'org.apache.commons'
        commonsLang.artifactId.text() == 'commons-lang3'
        commonsLang.version.text() == '3.3.2'
        commonsLang.scope.text() == 'provided'
    }

    def "Publishing provided dependencies to an Ivy repository preserves the scope"() {
        given:
        File repoUrl = new File(projectDir, 'build/repo')

        when:
        buildFile << """
apply plugin: 'java'
apply plugin: 'provided-base'
apply plugin: 'ivy-publish'

group = 'nebula.extraconf'
version '1.0'

repositories {
    mavenCentral()
}

dependencies {
    compile 'com.google.guava:guava:16.0'
    provided 'org.apache.commons:commons-lang3:3.3.2'
}

publishing {
    publications {
        ivyJava(IvyPublication) {
            from components.java
        }
    }

    repositories {
        ivy {
            url '$repoUrl.canonicalPath'
        }
    }
}
"""
        runTasksSuccessfully('publish')

        then:
        true
        File ivyFile = new File(repoUrl, "nebula.extraconf/$moduleName/1.0/ivy-1.0.xml")
        ivyFile.exists()
        def ivyXml = new XmlSlurper().parseText(ivyFile.text)
        def dependencies = ivyXml.dependencies
        dependencies.size() == 1
        def commonsLang = dependencies.dependency[1]
        commonsLang.@org.text() == 'org.apache.commons'
        commonsLang.@name.text() == 'commons-lang3'
        commonsLang.@rev.text() == '3.3.2'
        commonsLang.@conf.text() == 'provided'
    }

    @Ignore("https://github.com/nebula-plugins/gradle-extra-configurations-plugin/issues/14")
    def "Transitive dependencies in scope provided are not included in WAR archive"() {
        when:
        helper.addSubproject(
                "shared-component",
"""
apply plugin: 'java'
apply plugin: 'provided-base'

repositories {
    mavenCentral()
}

dependencies {
    provided 'org.apache.commons:commons-lang3:3.3.2'
}
"""
        )

        helper.addSubproject(
                "webapp-component",
"""
apply plugin: 'war'
apply plugin: 'provided-base'

repositories {
    mavenCentral()
}

dependencies {
    compile project(":shared-component")
}

task explodedWar(type: Copy) {
    into "\$buildDir/libs/exploded"
    with war
}
"""
        )

        runTasksSuccessfully('explodedWar')
        then:
        !new File(projectDir, 'webapp-component/build/libs/exploded/WEB-INF/lib/commons-lang3-3.3.2.jar').exists()
    }
}
