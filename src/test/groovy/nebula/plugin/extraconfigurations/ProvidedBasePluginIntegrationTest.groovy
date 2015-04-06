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

class ProvidedBasePluginIntegrationTest extends AbstractIntegrationTest {
    def setup() {
        buildFile << """
apply plugin: 'java'
apply plugin: 'provided-base'
"""
    }

    def "Can compile production code dependent on dependency declared as provided"() {
        when:
        buildFile << """
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

    def "Publishing provided dependencies to a Maven repository preserves the scope when using Maven Publish plugin"() {
        given:
        File repoUrl = new File(projectDir, 'build/repo')

        when:
        buildFile << """
apply plugin: 'maven-publish'

group = '$GROUP_ID'
version '$VERSION'

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
        assertProvidedDependencyInGeneratedPom(repoUrl, 'org.apache.commons', 'commons-lang3', '3.3.2')
    }

  def "Publishing provided dependencies to a Maven repository preserves the scope when using Maven plugin"() {
    given:
    File repoUrl = new File(projectDir, 'build/repo/')

    when:
    buildFile << """
apply plugin: 'maven'

group = '$GROUP_ID'
version '$VERSION'

repositories {
    mavenCentral()
}

dependencies {
    provided 'org.apache.commons:commons-lang3:3.3.2'
}

uploadArchives {
    repositories.mavenDeployer {
        repository(url: "file://$repoUrl.absolutePath")
    }
}
"""
        runTasksSuccessfully('install', 'uploadArchives')

        then:
        assertProvidedDependencyInGeneratedPom(MAVEN_LOCAL_DIR, 'org.apache.commons', 'commons-lang3', '3.3.2')
        assertProvidedDependencyInGeneratedPom(repoUrl, 'org.apache.commons', 'commons-lang3', '3.3.2')
    }

    def "Publishing provided dependencies to an Ivy repository preserves the scope"() {
        given:
        File repoUrl = new File(projectDir, 'build/repo')

        when:
        buildFile << """
apply plugin: 'ivy-publish'

group = '$GROUP_ID'
version '$VERSION'

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
        assertProvidedDependencyInGeneratedIvy(repoUrl, 'org.apache.commons', 'commons-lang3', '3.3.2')
    }

    def "Provided dependencies are not included in war archive"() {
        when:
        buildFile << """
apply plugin: 'war'
apply plugin: 'provided-base'

repositories {
    mavenCentral()
}

dependencies {
    provided 'org.apache.commons:commons-lang3:3.3.2'
}

task explodedWar(type: Copy) {
    into "\$buildDir/libs/exploded"
    with war
}
"""
        runTasksSuccessfully('explodedWar')

        then:
        !new File(projectDir, 'build/libs/exploded/WEB-INF/lib/commons-lang3-3.3.2.jar').exists()
    }

    def "Order of plugins declaration does not affect war content"() {
        when:
        buildFile << """
apply plugin: 'war'

repositories {
    mavenCentral()
}

dependencies {
    provided 'org.apache.commons:commons-lang3:3.3.2'
}

task explodedWar(type: Copy) {
    into "\$buildDir/libs/exploded"
    with war
}
"""
        runTasksSuccessfully('explodedWar')

        then:
        !new File(projectDir, 'build/libs/exploded/WEB-INF/lib/commons-lang3-3.3.2.jar').exists()
    }

    def "Transitive dependencies in scope provided are not included in war archive"() {
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
