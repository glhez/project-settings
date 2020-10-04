# Project Settings ![Build](https://github.com/glhez/project-settings/workflows/Build/badge.svg)

This repository contains project settings for my personal project (as found in the other repositories).

## Maven

The root project can be used to define common properties such as:

- dependencies
- plugins
- default configuration for some known plugins
- Java 8 / 11 / LATEST configuration

The LATEST here means the latest JDK.

The project is also configuring Eclipse project using the [eclipse-settings-maven-plugin][1].

## Eclipse

The Eclipse subproject ([eclipse/java8](eclipse/java8), [eclipse/java11](eclipse/java11)) must be imported as simple Eclipse project (the option is named _Import project into Workspace_ in the import list) rather than m2e project.

They can be edited like any classical Eclipse file: the [eclipse-settings-maven-plugin][1] is taking care of copying the relevant file into the target project.

## Generating a new version

New versions are automated by the release GitHub Action (following [GitHub Actions and Maven releases][4]):

1. A GPG Key must be generated: [Managing commit signature verification][3]
   1. `gpg --full-generate-key` to generate a new key
   2. `gpg --list-secret-keys --keyid-format LONG` to list available keys
   3. `gpg --export-secret-keys  --armor <KEY>` to export a key.
   4. The exported key must be in the project settings as `GPG_SIGNING_KEY_ARMOR` secret.
   5. The passphrase must also be added as secret.
2. [setup-java][5] will configure the `settings.xml`:
   1. A `github` `<server>` will be created for deployment, with GITHUB_ACTOR and GITHUB_TOKEN as user/password
   2. The provided GPG private key will be imported (the secret needs to use *armor*)
   3. The passphrase will also be configured in the settings.

The maven-gpg-plugin must be configured with the following, otherwise the passphrase will be ignored:

```xml
  <gpgArguments>
    <arg>--pinentry-mode</arg>
    <arg>loopback</arg>
  </gpgArguments>`
```

**Preparation**

Simply invoke `./mvnw release:prepare release:perform` and pick up a new version.


## Child project

Each project must define the following repositories:

- Plugin dependencies are fetched using `pluginRepository` rather than `repository`.
- Project should build without configuring further settings.

```xml
  <repositories>
    <repository>
      <id>github-glhez-repo-dep</id>
      <url>https://raw.githubusercontent.com/glhez/maven-repository/master/releases/</url>
      <snapshots> <enabled>false</enabled> </snapshots>
    </repository>
  </repositories>
  <pluginRepositories>
    <pluginRepository>
      <id>github-glhez-repo-plugin</id>
      <url>https://raw.githubusercontent.com/glhez/maven-repository/master/releases/</url>
      <snapshots> <enabled>false</enabled> </snapshots>
    </pluginRepository>
  </pluginRepositories>
```

[1]: https://github.com/BSI-Business-Systems-Integration-AG/eclipse-settings-maven-plugin
[2]: https://github.com/glhez/maven-repository
[3]: https://docs.github.com/en/free-pro-team@latest/github/authenticating-to-github/managing-commit-signature-verification
[4]: https://blog.frankel.ch/github-actions-maven-releases/
[5]: https://github.com/actions/setup-java

