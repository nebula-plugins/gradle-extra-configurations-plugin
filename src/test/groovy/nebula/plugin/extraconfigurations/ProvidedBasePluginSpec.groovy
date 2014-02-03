package nebula.plugin.extraconfigurations

import nebula.test.ProjectSpec
import org.gradle.api.plugins.JavaPluginConvention

class ProvidedBasePluginSpec extends ProjectSpec {
    @Override
    String getPluginName() {
        'nebula-provided-base'
    }

    def 'does provided conf exist'() {
        project.apply plugin: 'java'
        project.apply plugin: pluginName

        expect:
        project.configurations.provided != null
    }

    def 'order independent does provided conf exist'() {
        project.apply plugin: pluginName
        project.apply plugin: 'java'

        expect:
        project.configurations.provided != null
    }

    def 'check versions'() {
        project.apply plugin: 'java'
        project.apply plugin: 'netflix-repos'
        project.apply plugin: pluginName

        project.dependencies {
            compile 'com.google.guava:guava:12.0'
            provided 'commons-io:commons-io:2.4'
        }

        expect:
        def resolved = project.configurations.compile.resolvedConfiguration
        resolved.getResolvedArtifacts().any { it.name == 'guava' }
        resolved.getResolvedArtifacts().any { it.name == 'commons-io' }

        def resolvedProvided = project.configurations.provided.resolvedConfiguration
        !resolvedProvided.getResolvedArtifacts().any { it.name == 'guava' }
        resolvedProvided.getResolvedArtifacts().any { it.name == 'commons-io' }

        JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention)
        def main = javaConvention.sourceSets.main
        main.compileClasspath.any { it.name.contains 'guava'}
        main.compileClasspath.any { it.name.contains 'commons-io'}

        def test = javaConvention.sourceSets.test
        test.compileClasspath.any { it.name.contains 'guava'}
        test.compileClasspath.any { it.name.contains 'commons-io'}
    }
}
