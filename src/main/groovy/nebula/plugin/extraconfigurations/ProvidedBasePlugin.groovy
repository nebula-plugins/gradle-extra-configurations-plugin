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
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.maven.Conf2ScopeMappingContainer
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.MavenPlugin
import org.gradle.api.plugins.WarPlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.ivy.IvyPublication
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.bundling.War
import org.gradle.plugins.ide.eclipse.EclipsePlugin
import org.gradle.plugins.ide.idea.IdeaPlugin

class ProvidedBasePlugin implements Plugin<Project> {
    static final String PROVIDED_CONFIGURATION_NAME = 'provided'

    @Override
    void apply(Project project) {
        project.plugins.withType(JavaPlugin) {
            Configuration providedConfiguration = createProvidedConfiguration(project)
            configureIdeaPlugin(project, providedConfiguration)
            configureEclipsePlugin(project, providedConfiguration)
            configureMavenPublishPlugin(project, providedConfiguration)
            configureIvyPublishPlugin(project, providedConfiguration)
            configureWarPlugin(project, providedConfiguration)
            configureMavenPlugin(project, providedConfiguration)
        }
    }

    private Configuration createProvidedConfiguration(Project project) {
        Configuration compileConf = project.configurations.getByName(JavaPlugin.COMPILE_CONFIGURATION_NAME)

        // Our legacy provided scope, uber conf of provided and compile. This ensures what we're at least resolving with compile dependencies.
        def providedConf = project.configurations.create(PROVIDED_CONFIGURATION_NAME)
                .setVisible(true)
                .setTransitive(true)
                .setDescription('much like compile, but indicates that you expect the JDK or a container to provide it. It is only available on the compilation classpath, and is not transitive.')

        compileConf.extendsFrom(providedConf)

        // exclude provided dependencies when resolving dependencies between projects
        providedConf.allDependencies.all { Dependency dep ->
            project.configurations.default.exclude(group: dep.group, module: dep.name)
        }

        providedConf
    }

    /**
     * Configures the IDEA plugin to add the provided configuration to the PROVIDED scope.
     *
     * @param project Project
     * @param providedConfiguration Provided configuration
     */
    private void configureIdeaPlugin(Project project, Configuration providedConfiguration) {
        project.plugins.withType(IdeaPlugin) {
            project.idea.module {
                scopes.PROVIDED.plus += [providedConfiguration]
            }
        }
    }

    /**
     * Configures the Eclipse plugin to add the provided configuration.
     *
     * @param project Project
     * @param providedConfiguration Provided configuration
     */
    private void configureEclipsePlugin(Project project, Configuration providedConfiguration) {
        project.plugins.withType(EclipsePlugin) {
            project.eclipse.classpath.plusConfigurations += [ providedConfiguration ]
        }
    }

    /**
     * Configures Maven Publishing plugin to ensure that published dependencies receive the correct scope.
     *
     * @param project Project
     * @param providedConfiguration Provided configuration
     */
    private void configureMavenPublishPlugin(Project project, Configuration providedConfiguration) {
        project.plugins.withType(PublishingPlugin) {
            project.publishing {
                publications {
                    project.extensions.findByType(PublishingExtension)?.publications?.withType(MavenPublication) { MavenPublication pub ->
                        pub.pom.withXml {
                            // Replace dependency "runtime" scope element value with "provided"
                            asNode().dependencies.dependency.findAll {
                                it.scope.text() == JavaPlugin.RUNTIME_CONFIGURATION_NAME && providedConfiguration.allDependencies.find { dep ->
                                    dep.name == it.artifactId.text()
                                }
                            }.each { runtimeDep ->
                                runtimeDep.scope*.value = PROVIDED_CONFIGURATION_NAME
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
     * @param providedConfiguration Provided configuration
     */
    private void configureIvyPublishPlugin(Project project, Configuration providedConfiguration) {
        project.plugins.withType(PublishingPlugin) {
            project.publishing {
                publications {
                    project.extensions.findByType(PublishingExtension)?.publications?.withType(IvyPublication) { IvyPublication pub ->
                        pub.descriptor.withXml {
                            def rootNode = asNode()

                            // Add provided configuration if it doesn't exist yet
                            if (!rootNode.configurations.find { it.@name == PROVIDED_CONFIGURATION_NAME }) {
                                rootNode.configurations[0].appendNode('conf', [name: PROVIDED_CONFIGURATION_NAME, visibility: 'public'])
                            }

                            // Replace dependency "runtime->default" conf attribute value with "provided"
                            rootNode.dependencies.dependency.findAll {
                                it.@conf == "$JavaPlugin.RUNTIME_CONFIGURATION_NAME->default" && providedConfiguration.allDependencies.find { dep ->
                                    dep.name == it.@name
                                }
                            }.each { runtimeDep ->
                                runtimeDep.@conf = PROVIDED_CONFIGURATION_NAME
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Configures War plugin to ensure that provided dependencies are excluded from runtime classpath.
     *
     * @param project Project
     * @param providedConfiguration Provided configuration
     */
    private void configureWarPlugin(Project project, Configuration providedConfiguration) {
        project.plugins.withType(WarPlugin) {
            project.tasks.withType(War) {
                classpath = classpath.minus(providedConfiguration)
            }
        }
    }

    /**
     * Configures the Maven plugin to ensure that published dependencies receive the correct scope.
     *
     * @param project Project
     * @param providedConfiguration provided configuration
     */
    private void configureMavenPlugin(Project project, Configuration providedConfiguration) {
        project.plugins.withType(MavenPlugin) {
            project.conf2ScopeMappings.addMapping(MavenPlugin.COMPILE_PRIORITY + 1, providedConfiguration,
                                                  Conf2ScopeMappingContainer.PROVIDED)
        }
    }
}
