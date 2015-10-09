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
package nebula.plugin.extraconfigurations.publication
import org.gradle.api.Project
import org.gradle.api.publish.plugins.PublishingPlugin

abstract class AbstractPublishingConfigurer implements PublishingConfigurer {
    protected final Project project

    AbstractPublishingConfigurer(Project project) {
        this.project = project
    }

    @Override
    void withPublication(Closure closure) {
        Closure addArtifactClosure = {
            // Wait for our plugin to be applied.
            project.plugins.withType(PublishingPlugin) { PublishingPlugin publishingPlugin ->
                project.publishing {
                    publications.withType(publicationType, closure)
                }
            }
        }

        // It's possible that we're running in someone else's afterEvaluate, which means we need to run this immediately
        if(project.state.executed) {
            addArtifactClosure()
        }
        else {
            project.afterEvaluate addArtifactClosure
        }
    }
}
