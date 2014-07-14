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
package nebula.plugin.extraconfigurations.publication

import org.gradle.api.Project
import org.gradle.api.publish.internal.DefaultPublishingExtension
import org.gradle.api.publish.ivy.IvyPublication
import org.gradle.api.publish.ivy.plugins.IvyPublishPlugin

class IvyPublishingConfigurer implements PublishingConfigurer {
    private final Project project

    IvyPublishingConfigurer(Project project) {
        this.project = project
    }

    @Override
    void withPublication(Closure closure) {
        project.plugins.withType(IvyPublishPlugin) {
            project.publishing {
                publications {
                    DefaultPublishingExtension publishingExtension = project.extensions.getByType(DefaultPublishingExtension)
                    publishingExtension.publications.withType(IvyPublication) { IvyPublication publication ->
                        closure(publication.descriptor)
                    }
                }
            }
        }
    }
}
