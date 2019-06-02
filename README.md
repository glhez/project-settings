# Project Settings

This repository contains project settings for my personal project (as found in the other repositories).

## Maven

The root project can be used to define common properties such as:

- dependencies
- plugins
- default configuration for some known plugins
- Java 8 / 11 configuration

The project is also configuring Eclipse project using the [eclipse-settings-maven-plugin][1].

## Eclipse

The Eclipse subproject ([eclipse/java8](eclipse/java8), [eclipse/java11](eclipse/java11)) must be imported as simple Eclipse project (the option is named _Import project into Workspace_ in the import list) rather than m2e project.

They can be edited like any classical Eclipse file: the [eclipse-settings-maven-plugin][1] is taking care of copying the relevant file into the target project.
$
## Generating a new version

The version does not have much importance: I usually use the current day, in ISO order (eg: `2019.06.03`), then add some arbitrary number

**Preparation**

The companion repository [maven-repository][2] must be cloned before:

The `publish.directory` property **must** be  should be added to maven settings (`~/.m2/settings.xml`): the configuration below is adding this property to a new profile (which **must** be added to `<activeProfiles>`)

    <profile>
      <id>project-settings</id>
      <properties>
        <publish.directory>file:///e:/git/github/glhez-maven-repository</publish.directory>
      </properties>
    </profile>

Fix the URL based on your own settings.


**Step 1**

Simply invoke `./mvnw release:prepare release:perform` and pick up a new version.

**Step 2**

In the cloned [maven-repository][2], add the new artifacts and commit the whole.

[1]: https://github.com/BSI-Business-Systems-Integration-AG/eclipse-settings-maven-plugin
[2]: https://github.com/glhez/maven-repository



