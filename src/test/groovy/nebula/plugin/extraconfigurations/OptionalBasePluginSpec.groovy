package nebula.plugin.extraconfigurations

import nebula.test.PluginProjectSpec

class OptionalBasePluginSpec extends PluginProjectSpec {

    @Override
    String getPluginName() {
        'nebula.optional-base'
    }

    def setup() {
        project.apply plugin: pluginName
    }

    def "Applying the plugin create an extra property for holding the optional dependencies"() {
        expect:
        project.ext.has('optionalDeps')
        project.ext.optionalDeps == []
    }

    def "Can add optional external dependency with Map notation"() {
        when:
        project.configurations {
            myConf
        }

        project.dependencies {
            myConf group: 'commons-io', name: 'commons-io', version: '2.4', project.ext.optional
        }

        then:
        project.ext.optionalDeps.size() == 1
        def optionalDependency = project.ext.optionalDeps[0]
        optionalDependency.group == 'commons-io'
        optionalDependency.name == 'commons-io'
        optionalDependency.version == '2.4'
    }

    def "Can add optional external dependency with String notation"() {
        when:
        project.configurations {
            myConf
        }

        project.dependencies {
            myConf 'commons-io:commons-io:2.4', project.ext.optional
        }

        then:
        project.ext.optionalDeps.size() == 1
        def optionalDependency = project.ext.optionalDeps[0]
        optionalDependency.group == 'commons-io'
        optionalDependency.name == 'commons-io'
        optionalDependency.version == '2.4'
    }

    def "Can add mandatory and optional external dependencies"() {
        when:
        project.configurations {
            myConf
        }

        project.dependencies {
            myConf 'commons-lang:commons-lang:2.3'
            myConf 'commons-io:commons-io:2.4', project.ext.optional
            myConf group: 'log4j', name: 'log4j', version: '1.2.17'
        }

        then:
        project.ext.optionalDeps.size() == 1
        def optionalDependency = project.ext.optionalDeps[0]
        optionalDependency.group == 'commons-io'
        optionalDependency.name == 'commons-io'
        optionalDependency.version == '2.4'
    }

    def "Can combine optional with other operators"() {
        when:
        project.ext.excludeOptional = { dep ->
            exclude module: 'some-module'
            project.ext.optional(dep)
        }

        project.configurations {
            myConf
        }

        project.dependencies {
            myConf 'commons-io:commons-io:2.4', project.ext.excludeOptional
        }

        then:
        project.ext.optionalDeps.size() == 1
        def optionalDependency = project.ext.optionalDeps[0]
        optionalDependency.group == 'commons-io'
        optionalDependency.name == 'commons-io'
        optionalDependency.version == '2.4'
    }
}