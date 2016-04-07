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

import nebula.test.PluginProjectSpec
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedConfiguration
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.War
import org.gradle.util.GUtil
import spock.lang.Unroll

class ProvidedBasePluginSpec extends PluginProjectSpec {
    @Override
    String getPluginName() {
        'nebula.provided-base'
    }

    def "Does not create provided configuration if Java plugin is not applied"() {
        when:
        project.apply plugin: pluginName

        then:
        !project.configurations.findByName(ProvidedBasePlugin.PROVIDED_CONFIGURATION_NAME)
    }

    def 'Creates provided configuration if Java plugin is applied'() {
        when:
        project.apply plugin: 'java'
        project.apply plugin: pluginName

        then: 'Compile configuration extends from provided configuration'
        Configuration compileConfiguration = project.configurations.getByName(JavaPlugin.COMPILE_CONFIGURATION_NAME)
        compileConfiguration.extendsFrom.collect { it.name } as Set<String> == [ProvidedBasePlugin.PROVIDED_CONFIGURATION_NAME] as Set<String>
        !compileConfiguration.visible
        compileConfiguration.transitive

        and: 'Provided configuration exists and does not extend other configurations'
        Configuration providedConfiguration = project.configurations.getByName(ProvidedBasePlugin.PROVIDED_CONFIGURATION_NAME)
        providedConfiguration.extendsFrom == Collections.emptySet()
        providedConfiguration.visible
        providedConfiguration.transitive
    }

    @Unroll
    def 'Creates provided configuration for sourceSet "#sourceSetName"'() {
        when:
        project.apply plugin: 'java'
        project.apply plugin: pluginName
        project.sourceSets {
            foo
            bar
        }

        then: 'Compile configuration extends from provided configuration'
        String compileConfigurationName = project.sourceSets."$sourceSetName".compileConfigurationName
        String providedConfigurationName = compileConfigurationName.replace(GUtil.toCamelCase(JavaPlugin.COMPILE_CONFIGURATION_NAME), GUtil.toCamelCase(ProvidedBasePlugin.PROVIDED_CONFIGURATION_NAME))
        Configuration compileConfiguration = project.configurations.getByName(compileConfigurationName)
        compileConfiguration.extendsFrom.collect { it.name } as Set<String> == [providedConfigurationName] as Set<String>
        !compileConfiguration.visible
        compileConfiguration.transitive

        and: 'Provided configuration exists and does not extend other configurations'
        Configuration providedConfiguration = project.configurations.getByName(providedConfigurationName)
        providedConfiguration.extendsFrom == Collections.emptySet()
        providedConfiguration.visible
        providedConfiguration.transitive

        where:
        sourceSetName << ['foo', 'bar']
    }

    def 'order independent does provided conf exist'() {
        when:
        project.apply plugin: pluginName
        project.apply plugin: 'java'

        then:
        project.configurations.getByName(ProvidedBasePlugin.PROVIDED_CONFIGURATION_NAME)
    }

    def 'check versions'() {
        when:
        project.apply plugin: 'java'
        project.apply plugin: pluginName

        project.repositories {
            mavenCentral()
        }

        project.dependencies {
            compile 'com.google.guava:guava:12.0'
            provided 'commons-io:commons-io:2.4'
        }

        then:
        ResolvedConfiguration resolved = project.configurations.compile.resolvedConfiguration
        resolved.getResolvedArtifacts().any { it.name == 'guava' }
        resolved.getResolvedArtifacts().any { it.name == 'commons-io' }

        ResolvedConfiguration resolvedProvided = project.configurations.provided.resolvedConfiguration
        !resolvedProvided.getResolvedArtifacts().any { it.name == 'guava' }
        resolvedProvided.getResolvedArtifacts().any { it.name == 'commons-io' }

        JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention)
        SourceSet mainSourceSet = javaConvention.sourceSets.main
        mainSourceSet.compileClasspath.any { it.name.contains 'guava'}
        mainSourceSet.compileClasspath.any { it.name.contains 'commons-io'}

        SourceSet testSourceSet = javaConvention.sourceSets.test
        testSourceSet.compileClasspath.any { it.name.contains 'guava'}
        testSourceSet.compileClasspath.any { it.name.contains 'commons-io'}
    }

    @Unroll
    def "Dependency declared by configuration '#providedConfigurationName' is not added to War classpath if also defined by 'compile' configuration"() {
        when:
        project.apply plugin: 'war'
        project.apply plugin: pluginName
        project.repositories.mavenCentral()
        project.dependencies.add('compile', 'commons-io:commons-io:2.2')
        project.dependencies.add(providedConfigurationName, 'commons-io:commons-io:2.4')

        then:
        def resolved = project.configurations.getByName('compile').resolvedConfiguration
        resolved.getResolvedArtifacts().any { it.name == 'commons-io' && it.moduleVersion.id.version == '2.4' } // This is different from above

        def resolvedProvided = project.configurations.getByName(providedConfigurationName).resolvedConfiguration
        resolvedProvided.getResolvedArtifacts().any { it.name == 'commons-io' && it.moduleVersion.id.version == '2.4' }

        War warTask = project.tasks.getByName('war')
        def commonIos = warTask.classpath.findAll { it.name.contains 'commons-io' }
        commonIos.size() == 0

        where:
        providedConfigurationName << ['providedCompile', 'provided']
    }
}
