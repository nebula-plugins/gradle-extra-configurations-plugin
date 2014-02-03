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
        }
    }
}
