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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaPlugin

class ProvidedBasePlugin implements Plugin<Project> {
    private static Logger logger = Logging.getLogger(ProvidedBasePlugin)

    @Override
    void apply(Project project) {
        project.plugins.withType(JavaPlugin) {
            def compileConf = project.configurations.getByName(JavaPlugin.COMPILE_CONFIGURATION_NAME)

            // Our legacy provided scope, uber conf of provided and compile. This ensures what we're at least resolving with compile dependencies.
            def providedConf = project.configurations.create('provided')
                    .setVisible(true)
                    .setTransitive(true)
                    .setDescription('much like compile, but indicates that you expect the JDK or a container to provide it. It is only available on the compilation classpath, and is not transitive.')

            compileConf.extendsFrom(providedConf)

        // provided needs to be available to compile, runtime, testCompile, testRuntime
        // provided needs to be absent from ivy/pom
        // or for ivy -- conf provided
        // or for maven -- scope provided
        }
    }
}
