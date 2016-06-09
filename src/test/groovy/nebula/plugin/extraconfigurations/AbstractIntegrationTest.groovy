package nebula.plugin.extraconfigurations

import nebula.test.IntegrationSpec

abstract class AbstractIntegrationTest extends IntegrationSpec {
    public static final String GROUP_ID = 'nebula.extraconf'
    public static final String VERSION = '1.0'
    public static final File MAVEN_LOCAL_DIR = new File("${System.properties['user.home']}/.m2/repository")

    private String dirForGroup() {
        GROUP_ID.replaceAll('\\.', '/')
    }

    protected void assertProvidedDependencyInGeneratedPom(File repoUrl, String groupId, String artifactId, String version) {
        assertDependencyInGeneratedPom(repoUrl, groupId, artifactId, version, ProvidedBasePlugin.PROVIDED_CONFIGURATION_NAME)
    }

    protected void assertOptionalDependencyInGeneratedPom(File repoUrl, String groupId, String artifactId, String version, String scope) {
        assertDependencyInGeneratedPom(repoUrl, groupId, artifactId, version, scope, true)
    }

    private void assertDependencyInGeneratedPom(File repoUrl, String groupId, String artifactId, String version, String scope, Boolean optional = null) {
        File pomFile = new File(repoUrl, "${dirForGroup()}/$moduleName/$VERSION/$moduleName-${VERSION}.pom")
        assert pomFile.exists()
        def pomXml = new XmlSlurper().parseText(pomFile.text)
        def dependencies = pomXml.dependencies
        assert dependencies.size() == 1

        def dep = dependencies.dependency.find { dep ->
            dep.groupId.text() == groupId && dep.artifactId.text() == artifactId && dep.version.text() == version && dep.scope.text() == scope
        }

        assert dep

        if(optional) {
            assert dep.optional.text() == optional.toString()
        }
    }

    protected void assertProvidedDependencyInGeneratedIvy(File repoUrl, String groupId, String artifactId, String version) {
        assertDependencyInGeneratedIvy(repoUrl, groupId, artifactId, version, ProvidedBasePlugin.PROVIDED_CONFIGURATION_NAME)
    }

    protected void assertOptionalDependencyInGeneratedIvy(File repoUrl, String groupId, String artifactId, String version) {
        assertDependencyInGeneratedIvy(repoUrl, groupId, artifactId, version, OptionalBasePlugin.OPTIONAL_IDENTIFIER)
    }

    private void assertDependencyInGeneratedIvy(File repoUrl, String groupId, String artifactId, String version, String scope) {
        File ivyFile = new File(repoUrl, "$GROUP_ID/$moduleName/$VERSION/ivy-${VERSION}.xml")
        assert ivyFile.exists()
        def ivyXml = new XmlSlurper().parseText(ivyFile.text)
        def dependencies = ivyXml.dependencies
        assert dependencies.size() == 1

        assert dependencies.dependency.find { dep ->
            dep.@org.text() == groupId && dep.@name.text() == artifactId && dep.@rev.text() == version && dep.@conf.text() == scope
        }
    }
}
