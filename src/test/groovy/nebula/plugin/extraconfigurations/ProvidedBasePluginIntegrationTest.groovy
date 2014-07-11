package nebula.plugin.extraconfigurations

import nebula.test.IntegrationSpec

class ProvidedBasePluginIntegrationTest extends IntegrationSpec {
    def "Can compile production code dependent on dependency declared as provided"() {
        when:
        buildFile << """
apply plugin: 'java'
apply plugin: 'nebula-provided-base'

repositories {
    mavenCentral()
}

dependencies {
    provided 'org.apache.commons:commons-lang3:3.3.2'
}
"""
        createProductionJavaSourceFile()
        runTasksSuccessfully('compileJava')

        then:
        new File(projectDir, 'build/classes/main/nebula/extraconf/HelloWorld.class').exists()
    }

    def "Can test production code dependent on dependency declared as provided"() {
        when:
        buildFile << """
apply plugin: 'java'
apply plugin: 'nebula-provided-base'

repositories {
    mavenCentral()
}

dependencies {
    provided 'org.apache.commons:commons-lang3:3.3.2'
    testCompile 'junit:junit:4.8.2'
}
"""
        createProductionJavaSourceFile()
        createTestSourceJavaClass()
        runTasksSuccessfully('test')

        then:
        new File(projectDir, 'build/classes/main/nebula/extraconf/HelloWorld.class').exists()
        new File(projectDir, 'build/classes/test/nebula/extraconf/HelloWorldTest.class').exists()
        new File(projectDir, 'build/test-results/TEST-nebula.extraconf.HelloWorldTest.xml').exists()
    }

    private void createProductionJavaSourceFile() {
        File javaFile = createFile("src/main/java/nebula/extraconf/HelloWorld.java")
        javaFile << """
package nebula.extraconf;

import org.apache.commons.lang3.StringUtils;

public class HelloWorld {
    public String getMessage() {
        return StringUtils.upperCase("Hello World!");
    }
}
"""
    }

    private void createTestSourceJavaClass() {
        File javaTestFile = createFile('src/test/java/nebula/extraconf/HelloWorldTest.java')
        javaTestFile << """
package nebula.extraconf;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class HelloWorldTest {
    @Test
    public void getMessage() {
        assertEquals("HELLO WORLD!", new HelloWorld().getMessage());
    }
}
"""
    }

    def "Adds provided dependencies to PROVIDED scope if Idea plugin is applied"() {
        when:
        buildFile << """
apply plugin: 'java'
apply plugin: 'nebula-provided-base'
apply plugin: 'idea'

repositories {
    mavenCentral()
}

dependencies {
    provided 'org.apache.commons:commons-lang3:3.3.2'
}
"""
        runTasksSuccessfully('idea')

        then:
        File ideaModuleFile = new File(projectDir, "${moduleName}.iml")
        ideaModuleFile.exists()
        def moduleXml = new XmlSlurper().parseText(ideaModuleFile.text)
        def orderEntries = moduleXml.component.orderEntry.findAll { it.@type.text() == 'module-library' && it.@scope.text() == 'PROVIDED' }
        orderEntries.find { it.library.CLASSES.root.@url.text().contains('commons-lang3-3.3.2.jar') }
    }
}
