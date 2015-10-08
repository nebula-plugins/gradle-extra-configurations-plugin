/*
 * Copyright 2014-2015 Netflix, Inc.
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
import nebula.test.dependencies.DependencyGraph
import nebula.test.dependencies.DependencyGraphBuilder
import nebula.test.dependencies.GradleDependencyGenerator
import nebula.test.functional.ExecutionResult

class OptionalBasePluginIntegrationTest extends AbstractIntegrationTest {
    def setup() {
        buildFile << """
apply plugin: 'java'
apply plugin: 'nebula.optional-base'
"""
    }

    def "Can use optional"() {
        given:
        File baseDir = new File(projectDir, 'build')
        File mavenRepoDir = new File(baseDir, 'mavenrepo')
        def generator = new GradleDependencyGenerator(new DependencyGraph(['foo:bar:2.4 -> custom:baz:5.1.27', 'custom:baz:5.1.27']), baseDir.canonicalPath)
        generator.generateTestMavenRepo()

        when:
        buildFile << """
repositories {
    maven { url '$mavenRepoDir.canonicalPath' }
}

dependencies {
    compile 'foo:bar:2.4', optional
}
"""
        ExecutionResult result = runTasksSuccessfully('dependencies')

        then:
        result.standardOutput.contains("""
compile - Compile classpath for source set 'main'.
\\--- foo:bar:2.4
     \\--- custom:baz:5.1.27

default - Configuration for default artifacts.
\\--- foo:bar:2.4
     \\--- custom:baz:5.1.27

runtime - Runtime classpath for source set 'main'.
\\--- foo:bar:2.4
     \\--- custom:baz:5.1.27

testCompile - Compile classpath for source set 'test'.
\\--- foo:bar:2.4
     \\--- custom:baz:5.1.27

testRuntime - Runtime classpath for source set 'test'.
\\--- foo:bar:2.4
     \\--- custom:baz:5.1.27
""")
    }

    def "Can combine optional with other operators"() {
        given:
        File baseDir = new File(projectDir, 'build')
        File mavenRepoDir = new File(baseDir, 'mavenrepo')
        def generator = new GradleDependencyGenerator(new DependencyGraph(['foo:bar:2.4 -> custom:baz:5.1.27', 'custom:baz:5.1.27']), baseDir.canonicalPath)
        generator.generateTestMavenRepo()

        when:
        buildFile << """
ext.excludeOptional = { dep ->
    exclude module: 'baz'
    optional(dep)
}

repositories {
    maven { url '$mavenRepoDir.canonicalPath' }
}

dependencies {
    compile 'foo:bar:2.4', excludeOptional
}
"""
        ExecutionResult result = runTasksSuccessfully('dependencies')

        then:
        result.standardOutput.contains("""
compile - Compile classpath for source set 'main'.
\\--- foo:bar:2.4

default - Configuration for default artifacts.
\\--- foo:bar:2.4

runtime - Runtime classpath for source set 'main'.
\\--- foo:bar:2.4

testCompile - Compile classpath for source set 'test'.
\\--- foo:bar:2.4

testRuntime - Runtime classpath for source set 'test'.
\\--- foo:bar:2.4
""")
        !result.standardOutput.contains('custom:baz:5.1.27')
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
    compile 'org.apache.commons:commons-lang3:3.3.2', optional
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
        assertOptionalDependencyInGeneratedPom(repoUrl, 'org.apache.commons', 'commons-lang3', '3.3.2', 'runtime')
    }

    def "Publishing provided dependencies to a Maven repository preserves the scope when using Maven plugin"() {
        given:
        File repoUrl = new File(projectDir, 'build/repo')

        when:
        buildFile << """
apply plugin: 'maven'

group = '$GROUP_ID'
version '$VERSION'

repositories {
    mavenCentral()
}

dependencies {
    compile 'org.apache.commons:commons-lang3:3.3.2', optional
}

uploadArchives {
    repositories {
        mavenDeployer {
            repository(url: 'file://$repoUrl.canonicalPath')
        }
    }
}
"""
        runTasksSuccessfully('install', 'uploadArchives')

        then:
        assertOptionalDependencyInGeneratedPom(MAVEN_LOCAL_DIR, 'org.apache.commons', 'commons-lang3', '3.3.2', 'compile')
        assertOptionalDependencyInGeneratedPom(repoUrl, 'org.apache.commons', 'commons-lang3', '3.3.2', 'compile')
    }

    def "Publishing optional dependencies to an Ivy repository preserves the scope"() {
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
    compile 'org.apache.commons:commons-lang3:3.3.2', optional
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
        assertOptionalDependencyInGeneratedIvy(repoUrl, 'org.apache.commons', 'commons-lang3', '3.3.2')
    }

    def 'still works if maven-publish publication is modified in after evaluate'() {
        given:
        def graph = new DependencyGraphBuilder().addModule('test.nebula:foo:1.0.0').build()
        File mavenRepo = new GradleDependencyGenerator(graph, "${projectDir}/testrepogen").generateTestMavenRepo()
        File repoUrl = new File(projectDir, 'build/repo')

        buildFile << """\
            apply plugin: 'maven-publish'

            group = '$GROUP_ID'
            version = '$VERSION'

            repositories { maven { url '${mavenRepo.absolutePath}' } }

            dependencies {
                compile 'test.nebula:foo:1.0.0', optional
            }

            afterEvaluate {
                publishing {
                    repositories {
                        maven {
                            name 'testRepo'
                            url '${repoUrl.canonicalPath}'
                        }
                    }
                    publications {
                        testMaven(MavenPublication) {
                            from components.java
                        }
                    }
                }
            }
        """.stripIndent()

        when:
        runTasksSuccessfully('publish')

        then:
        assertOptionalDependencyInGeneratedPom(repoUrl, 'test.nebula', 'foo', '1.0.0', 'runtime')
    }
}
