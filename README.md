# Project Settings

This repository contains project settings for my personal project (as found in the other repositories).

## Maven

The root project can be used to define common properties such as:

- dependencies
- plugins
- default configuration for some known plugins
- Java 8 / 11 configuration

The project is also configuring Eclipse project using the  [eclipse-settings-maven-plugin][1].

## Eclipse

The Eclipse subproject must be imported as simple Eclipse project (the option is named _Import project into Workspace_ in the import list) rather than m2e project.

They can be edited like any classical Eclipse file: the [eclipse-settings-maven-plugin][1] is taking care of copying the relevant file into the target project.

[1]: https://github.com/BSI-Business-Systems-Integration-AG/eclipse-settings-maven-plugin

## Generating a new version

Version are named after the day they are published. A number is added in case of problems.

