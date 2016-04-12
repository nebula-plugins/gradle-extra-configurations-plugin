/*
 * Copyright 2014-2016 Netflix, Inc.
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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.MavenPlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.ivy.IvyPublication
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.plugins.PublishingPlugin

class OptionalBasePlugin implements Plugin<Project> {
    static final String OPTIONAL_IDENTIFIER = 'optional'

    @Override
    void apply(Project project) {
        enhanceProjectModel(project)
        configureMavenPublishPlugin(project)
        configureIvyPublishPlugin(project)
        configureMavenPlugin(project)
    }

    /**
     * Enhances the Project domain object by adding
     *
     * a) a extra property List that holds optional dependencies
     * b) a extra method that can be executed as parameter when declaring dependencies
     *
     * @param project Project
     */
    private void enhanceProjectModel(Project project) {
        project.ext.optionalDeps = []

        project.ext.optional = { dep ->
            project.ext.optionalDeps << dep
        }
    }

    /**
     * Configures Maven Publishing plugin to ensure that published dependencies receive the optional element.
     *
     * @param project Project
     */
    private void configureMavenPublishPlugin(Project project) {
        project.plugins.withType(PublishingPlugin) {
            project.publishing {
                publications {
                    project.extensions.findByType(PublishingExtension)?.publications?.withType(MavenPublication) { MavenPublication pub ->
                        pub.pom.withXml {
                            project.ext.optionalDeps.each { dep ->
                                def foundDep = asNode().dependencies.dependency.find {
                                    it.artifactId.text() == dep.name
                                }

                                if (foundDep) {
                                    if (foundDep.optional) {
                                        foundDep.optional.value = 'true'
                                    } else {
                                        foundDep.appendNode(OPTIONAL_IDENTIFIER, 'true')
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Configures Ivy Publishing plugin to ensure that published dependencies receive the correct conf attribute value.
     *
     * @param project Project
     */
    private void configureIvyPublishPlugin(Project project) {
        project.plugins.withType(PublishingPlugin) {
            project.publishing {
                publications {
                    project.extensions.findByType(PublishingExtension)?.publications?.withType(IvyPublication) { IvyPublication pub ->
                        pub.descriptor.withXml {
                            def rootNode = asNode()

                            // Add optional configuration if it doesn't exist yet
                            if (!rootNode.configurations.find { it.@name == OPTIONAL_IDENTIFIER }) {
                                rootNode.configurations[0].appendNode('conf', [name: OPTIONAL_IDENTIFIER, visibility: 'public'])
                            }

                            // Replace dependency "runtime->default" conf attribute value with "optional"
                            project.ext.optionalDeps.each { dep ->
                                def foundDep = rootNode.dependencies.dependency.find { it.@name == dep.name }
                                foundDep?.@conf = OPTIONAL_IDENTIFIER
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Configures Maven plugin to ensure that published dependencies receive the optional element.
     *
     * @param project Project
     */
    private void configureMavenPlugin(Project project) {
        project.plugins.withType(MavenPlugin) {
            // Requires user definition of Maven installer/deployer which could be anywhere in the build script
            project.afterEvaluate {
                def installers = project.tasks.install.repositories
                def deployers = project.tasks.uploadArchives.repositories

                installers.plus(deployers)*.pom*.whenConfigured { pom ->
                    project.ext.optionalDeps.each { optionalDep ->
                        pom.dependencies.find {
                            dep -> dep.groupId == optionalDep.group && dep.artifactId == optionalDep.name
                        }.optional = true
                    }
                }
            }
        }
    }
}
