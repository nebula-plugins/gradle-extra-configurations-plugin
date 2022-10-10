Nebula Extra Configurations
===========================

![Support Status](https://img.shields.io/badge/nebula-active-green.svg)
[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com.netflix.nebula/gradle-extra-configurations-plugin/maven-metadata.xml.svg?label=gradlePluginPortal)](https://plugins.gradle.org/plugin/com.netflix.nebula.extra-configurations)
[![Maven Central](https://img.shields.io/maven-central/v/com.netflix.nebula/gradle-extra-configurations-plugin)](https://maven-badges.herokuapp.com/maven-central/com.netflix.nebula/gradle-extra-configurations-plugin)
![Build](https://github.com/nebula-plugins/gradle-extra-configurations-plugin/actions/workflows/nebula.yml/badge.svg)
[![Apache 2.0](https://img.shields.io/github/license/nebula-plugins/gradle-extra-configurations-plugin.svg)](http://www.apache.org/licenses/LICENSE-2.0)


This plugin allows dependencies to be declared with a configuration or attribute not available in Gradle core.

* `optional`

The following publishing plugins support the correct handling when generating the relevant metadata:

* [Maven Publishing](http://www.gradle.org/docs/current/userguide/publishing_maven.html)
* [Ivy Publishing](http://www.gradle.org/docs/current/userguide/publishing_ivy.html)

## Usage

### Adding the plugin binary to the build

To include, add the following to your build.gradle

    plugins {
      id 'com.netflix.nebula.optional-base' version '3.0.3' // if you want optional-base
      id 'com.netflix.nebula.provided-base' version '3.0.3' // if you want provided-base
    }

or

    buildscript {
        repositories { mavenCentral() }

        dependencies {
            classpath 'com.netflix.nebula:gradle-extra-configurations-plugin:3.0.3'
        }
    }

The JAR file comes with two plugins:

<table>
    <tr>
        <th>Plugin Identifier</th>
        <th>Depends On</th>
        <th>Type</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>provided-base</td>
        <td>-</td>
        <td>ProvidedBasePlugin</td>
        <td>Creates a new configuration named provided similar to providedCompile created by the
        <a href="http://www.gradle.org/docs/current/userguide/war_plugin.html">War plugin</a>.</td>
    </tr>
    <tr>
        <td>optional-base</td>
        <td>-</td>
        <td>OptionalBasePlugin</td>
        <td>Provides an extra method for marking dependencies as optional.</td>
    </tr>
</table>

To use the Provided plugin, include the following code snippet in your build script:

    apply plugin: 'com.netflix.nebula.provided-base'

To use the Optional plugin, include the following code snippet in your build script:

    apply plugin: 'com.netflix.nebula.optional-base'

### Using the provided plugin

A dependency declared with the `provided` configuration is available on the compilation and test classpath. However, when
publishing the outgoing artifact of the project (usually a JAR file) with Ivy or Maven, the dependency is not marked as
required transitive dependency.

This is what the [Maven documentation](http://maven.apache.org/pom.html#Dependencies) says:

> **provided** - this is much like compile, but indicates you expect the JDK or a container to provide it at runtime.
> It is only available on the compilation and test classpath, and is not transitive.

#### Impact on metadata created when publishing artifact

When publishing a provided dependency to a Maven repository the declaration in the POM looks as such:

    <project>
        ...
        <dependencies>
            <dependency>
                <groupId>org.apache.commons</groupId>
                <artifactId>commons-lang3</artifactId>
                <version>3.3.2</version>
                <scope>provided</scope>
            </dependency>
        </dependencies>
        ...
    </project>

When publishing a provided dependency to an Ivy repository the declaration in the Ivy file looks as such:

    <ivy-module>
        <configurations>
            ...
            <conf name="provided" visibility="public"/>
        </configurations>
        ...
        <dependencies>
            <dependency org="org.apache.commons" name="commons-lang3" rev="3.3.2" conf="provided"/>
        </dependencies>
    </ivy-module>

#### Usage example

    apply plugin: 'java'
    apply plugin: 'com.netflix.nebula.provided-base'

    repositories {
        mavenCentral()
    }

    dependencies {
        provided 'org.apache.commons:commons-lang3:3.3.2'
        provided group: 'log4j', name: 'log4j', version: '1.2.17'
    }

### Using the optional plugin

A dependency marked with the `optional` attribute is a dependency that is not necessarily meant to be required. The idea
is that some of the dependencies are only used for certain features in the project, and will not be needed if that feature
isn't used. For more information, please have a look at the [Maven documentation](http://maven.apache.org/guides/introduction/introduction-to-optional-and-excludes-dependencies.html).

#### Impact on metadata created when publishing artifact

When publishing a optional dependency to a Maven repository the declaration in the POM looks as such:

    <project>
        ...
        <dependencies>
            <dependency>
                <groupId>org.apache.commons</groupId>
                <artifactId>commons-lang3</artifactId>
                <version>3.3.2</version>
                <scope>compile</scope>
                <optional>true</optional>
            </dependency>
        </dependencies>
        ...
    </project>

When publishing a optional dependency to an Ivy repository the declaration in the Ivy file looks as such:

    <ivy-module>
        <configurations>
            ...
            <conf name="optional" visibility="public"/>
        </configurations>
        ...
        <dependencies>
            <dependency org="org.apache.commons" name="commons-lang3" rev="3.3.2" conf="optional"/>
        </dependencies>
    </ivy-module>

#### Usage example

    apply plugin: 'java'
    apply plugin: 'com.netflix.nebula.optional-base'

    repositories {
        mavenCentral()
    }

    dependencies {
        compile 'org.apache.commons:commons-lang3:3.3.2', optional
        compile group: 'log4j', name: 'log4j', version: '1.2.17', optional
    }
