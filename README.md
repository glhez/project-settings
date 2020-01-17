# Eclipse Settings maven-plugin [![Build Status](https://travis-ci.org/glhez/eclipse-settings-maven-plugin.svg?branch=rebrand)](https://travis-ci.org/glhez/eclipse-settings-maven-plugin)

Provide consistent Eclipse IDE settings for your team from a Maven POM.
The eclipse-settings-maven-plugin will copy formatting, findbugs and other plugin
settings from a centrally maintained settings JAR to your checked out workspace and
configure each project to use those settings.

 - configure once, set everywhere
 - version control your settings

Many thanks to [Olivier Nouguier](https://github.com/cheleb) for the
[first version of this plugin](https://github.com/cheleb/m2e-settings).
And many thanks to [Martijn Dashorst](https://github.com/dashorst) for the
[second version of this plugin](https://github.com/topicusonderwijs/m2e-settings).

This project is licensed under the [MIT license](https://github.com/BSI-Business-Systems-Integration-AG/eclipse-settings-maven-plugin/blob/bsi_release/LICENSE.txt).

# About this fork

I decided to create the fork because I had several problems with the plugin; while it is great, it lacks one fundamental thing: you don't know what file is being copied and from where.

Since, I had configuration problems, as well as the [second version of this plugin](https://github.com/topicusonderwijs/m2e-settings), I really wanted to know what was being done:

- The second version plugin was "catching" this plugin execution, leading to issues because it was expecting older maven-eclipse-plugin configuration.
- After this "second version" Eclipse plugin was removed, the properties were still not properly updated.

This fork will:

- Maven related change:
  - `groupId` is changed to `com.github.glhez` to avoid avoid name clash (and also because I'm not releasing versions for `org.eclipse.scout`).
  - `version` is changed from `3.0.4-SNAPSHOT` to `3.2.x`.
- Important technical change:
  - Plugins and dependencies are updated to Maven 3.6.0.
- Minor code change:
  - Add more logs to know what the plugin does.
  - Use [BuildContext](https://www.eclipse.org/m2e/documentation/m2e-making-maven-plugins-compat.html#buildcontext-code-snippets) to "warn" Eclipse update file update.
- Feature change
  - Added `failOnError` whose default value is `true` to fail if a file could not be copied (ex: file missing, ...). **Can be set per file.**
  - Added `packaging` to filter file based on packaging. This allow customized what should be copied based on packaging (ex: pom file should not have java settings). **Can be set per file.**
  - Use `<source>` to select a resolver: it will resolve `<location>` based on that.

**Note:**

1. Version are released in my [Github maven repository][3]. That's probably not a "good" idea to do that, but I don't have time to publish it on Central.
2. While Maven 3.6.1 is out there, there are dependencies resolving conflicts (as of now) with maven-tycho-plugin 1.4 and maven 3.6.1.
3. The version of Java is fixed to Java 7 to match Maven 3.6.0 version (see [Maven Releases History](http://maven.apache.org/docs/history.html))

# Table of content

- [Eclipse Settings maven-plugin ![Build Status](https://travis-ci.org/glhez/eclipse-settings-maven-plugin)](#eclipse-settings-maven-plugin-img-src%22httpstravis-ciorgglhezeclipse-settings-maven-plugin%22-alt%22build-status%22)
- [About this fork](#about-this-fork)
- [Table of content](#table-of-content)
- [Configuration](#configuration)
  - [Add the maven repository](#add-the-maven-repository)
  - [Create your own settings jar](#create-your-own-settings-jar)
    - [Create a Maven project](#create-a-maven-project)
    - [Add your settings to the JAR](#add-your-settings-to-the-jar)
    - [Deploy to a Maven repository](#deploy-to-a-maven-repository)
  - [Configure Eclipse Settings maven-plugin in your project](#configure-eclipse-settings-maven-plugin-in-your-project)
    - [Putting the settings in the right place](#putting-the-settings-in-the-right-place)
    - [Skipping the plugin execution](#skipping-the-plugin-execution)
  - [Re-import projects in Eclipse](#re-import-projects-in-eclipse)
- [Releasing](#releasing)

# Configuration

There are three steps to configure the *Eclipse Settings maven-plugin*:

1. Create (and deploy) your own settings jar
2. Configure the *Eclipse Settings maven-plugin* in your project
3. Re-import the Maven projects in Eclipse

## Add the maven repository

The `com.github.glhez:eclipse-settings-maven-plugin` is not on maven central: you will have to add this repository to your pom, settings or enterprise Maven repository (such as Nexus or Artifactory):

- You should _really_ add it to your enterprise repository so that your build stay consistent (for example, it may be removed).
- Adding to your pom can also be a good idea, but you should probably add an URL to your enterprise repository if possible.

``` xml
  <repositories>
    <repository> <id>github-maven-parent</id>      <url>https://raw.githubusercontent.com/glhez/maven-repository/master/releases/</url> </repository>
  </repositories>
```

## Create your own settings jar

Create a project for your own settings jar. This project will only
contain the relevant Eclipse settings files for your plugins.

### Create a Maven project

First create an empty Maven project, and put this in the POM to build
your settings jar (adjust the values for your own settings jar).

``` xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <prerequisites>
        <maven>3.3.1</maven>
    </prerequisites>
    <groupId>com.example.settings</groupId>
    <artifactId>eclipse-settings</artifactId>
    <packaging>jar</packaging>
    <build>
        <defaultGoal>package</defaultGoal>
        <resources>
            <resource>
                <directory>files</directory>
                <filtering>false</filtering>
                <includes>
                    <include>**/*</include>
                </includes>
            </resource>
        </resources>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

This configures Maven to look in the `files` folder for resources and
package them into the resulting jar.

### Add your settings to the JAR

Now you can copy the various Eclipse settings from the `.settings`
folders into the files folder:

``` bash
$ ls settings-project/files
-rw-r--r--   1 dashorst  staff     55 Jul  7 17:52 edu.umd.cs.findbugs.plugin.eclipse.prefs
-rw-r--r--   1 dashorst  staff    529 Jul  7 17:52 org.eclipse.core.resources.prefs
-rw-r--r--   1 dashorst  staff    175 Jul  7 17:52 org.eclipse.jdt.apt.core.prefs
-rw-r--r--   1 dashorst  staff  31543 Jul  7 17:52 org.eclipse.jdt.core.prefs
-rw-r--r--   1 dashorst  staff  11723 Jul  7 17:52 org.eclipse.jdt.ui.prefs
-rw-r--r--   1 dashorst  staff     86 Jun 29 23:47 org.eclipse.m2e.core.prefs
-rw-r--r--   1 dashorst  staff    411 Jun 29 23:52 org.eclipse.wst.common.component
-rw-r--r--   1 dashorst  staff    167 Jun 29 23:52 org.eclipse.wst.common.project.facet.core.xml
-rw-r--r--   1 dashorst  staff    382 Jul  7 17:52 org.eclipse.wst.validation.prefs
-rw-r--r--   1 dashorst  staff    232 Jul  7 17:52 org.maven.ide.eclipse.prefs
```

You can repeat this every time a new version of Eclipse comes out, and
update all settings to new defaults.

### Deploy to a Maven repository

Now you can upload the jar to a Maven repository using `mvn deploy`. Or
use the Maven release plugin to create releases of your settings jar.

## Configure Eclipse Settings maven-plugin in your project

The eclipse-settings-maven-plugin retrieves the Eclipse workspace settings from
its configuration which is similar to the [Maven Eclipse Plugin][1] configuration.
The easiest way to provide these settings is to create a resource JAR file and distribute that
from a Maven repository.

You then specify your 'settings JAR' file as a dependency to the
*eclipse-settings-maven-plugin*:

``` xml
<build>
    <pluginManagement>
        <plugins>
            ...
            <plugin>
                <groupId>com.github.glhez</groupId>
                <artifactId>eclipse-settings-maven-plugin</artifactId>
                <version>LATEST</version> <!-- see pom.xml -->
                <dependencies>
                    <dependency>
                        <groupId>com.example.settings</groupId>
                        <artifactId>eclipse-settings</artifactId>
                        <version>1.0</version>
                    </dependency>
                </dependencies>
            </plugin>
            ...
        </plugins>
    </pluginManagement>
</build>
```

This is not specific to this plugin at all, but do not forget that maven uses all declared `pluginRepository` entries to fetch dependencies for plugins. If the JAR you are getting the pref files from is not present in maven central (which is used by default), then you will need to add a `<pluginRepositories>..</pluginRepositories>` section in your POM or in your `settings.xml` file.

As the plugin needs to be bound to a Maven lifecycle you also need to
specify the eclipse-settings-maven-plugin in your build. At the minimum you'll
need:

- The profile eclipse-settings is activated only if the m2e.version property is defined; which is always the case in Eclipse.
- No doing so will copy settings for each module in the reactor, rather than those that are effectively imported into Eclipse: this will generate garbage files.

``` xml
  <profiles>
    <profile>
      <id>eclipse-settings</id>
      <activation> <property> <name>m2e.version</name> </property> </activation>
      <build>
        <plugins>
          <plugin>
            <groupId>com.github.glhez</groupId>
            <artifactId>eclipse-settings-maven-plugin</artifactId>
            <executions>
              <execution>
                <id>attach-eclipse-settings</id>
                <goals>
                  <goal>eclipse-settings</goal>
                </goals>
              </execution>
            </executions>
          </plugin>
        </plugins>
      <profile>
    </profiles>
```

### Putting the settings in the right place

The *eclipse-settings-maven-plugin* allows you to [copy settings files from one
location to another][2]. You use that to put each configuration file
from your settings JAR in the right location:

``` xml
<build>
    <pluginManagement>
        <plugins>
            ...
            <plugin>
            <...>
            <configuration>
                <failOnError>true</failOnError>  <!-- this is the default -->
                <packaging>!pom</packaging>      <!-- this is the default -->
                <!-- where the configuration is. -->
                <source>jar:/</source>
                <!-- <source>file:${session.executionRootDirectory}</source> -->
                <additionalConfig>
                    <file>
                        <name>.settings/org.eclipse.jdt.core.prefs</name>
                        <location>/org.eclipse.jdt.core.prefs</location>
                        <packaging>jar</packaging>
                    </file>
                    <file>
                        <name>.settings/org.eclipse.jdt.ui.prefs</name>
                        <location>/org.eclipse.jdt.ui.prefs</location>
                        <packaging>war</packaging>
                        <failOnError>false</failOnError>
                    </file>
                    <!-- and more... -->
                </additionalConfig>
            </configuration>
            </plugin>
            ...
        </plugins>
    </pluginManagement>
</build>
```

Both `localAdditionalConfig` and `additionalConfig` does the same: copy the content of file represented by `location` to the file/path represented by `name`.

- `additionalConfig` will resolve files in the class path, requiring one or more plugin dependencies.
- `localAdditionalConfig`will resolves files in the file system.
  - `${session.executionRootDirectory}` represents the "current working directory" in which maven is installed. Thus, probably the root.
  - You should probably use a more stable path (for example, if you compile a subproject, the `${session.executionRootDirectory}` will correspond to the folder in which this project is, not its parent).

Files that could not be copied or were not found will fail with an error.

### Skipping the plugin execution

The plugin has a 'skip' configuration parameter to block the configuration of a project.
This can be useful to disable a configuration made in the the parent pom at child pom level.
Example:

``` xml
<build>
  <plugins>
    ...
    <plugin>
      <groupId>com.github.glhez</groupId>
      <artifactId>eclipse-settings-maven-plugin</artifactId>
      <configuration>
        <skip>true</skip>
      </configuration>
    </plugin>
    ...
  </plugins>
</build>
```

## Re-import projects in Eclipse

Now we have modified the projects, you have to re-import the projects
in Eclipse. Typically this is done by:

 - selecting all projects,
 - right-clicking on the selection and
 - clicking "Maven → Update project"

# Releasing

To release, you need to define the following profile in your settings:

    <profile>
      <id>project-settings</id>
      <properties>
        <gpg.github.keyname><!--your key email --></gpg.github.keyname>

        <publish.directory>file:///e:/git/github/glhez-maven-repository</publish.directory>
      </properties>
    </profile>

Since the JAR are signed using maven-gpg-plugin, you need to create a new key: you may want to read this two documentations:

- [How to Generate PGP Signatures with Maven](https://blog.sonatype.com/2010/01/how-to-generate-pgp-signatures-with-maven/)
- [OpenPGP Web Key Directory (WKD) hosting](https://sizeof.cat/post/openpgp-web-key-directory-wkd-hosting)

Assuming you created said key, then here is what you'll need to do next:

- `gpg.github.keyname` correspond to the email associated with the key (that is used by gpg to find the key).
- `publish.directory` is the path to some directory on your filesystem. This directory could be versioned (in my case, it point to my [repository][3]).

After all said, you only have to invoke maven:

    ./mvnw release:perform release:prepare

This should work.

[1]: http://maven.apache.org/plugins/maven-eclipse-plugin
[2]: http://maven.apache.org/plugins/maven-eclipse-plugin/eclipse-mojo.html#additionalConfig
[3]: https://github.com/glhez/maven-repository
