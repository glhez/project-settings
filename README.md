# Project Settings ![Build](https://github.com/glhez/project-settings/workflows/Build/badge.svg)

This repository contains project settings for my personal project (as found in the other repositories).

## Maven

The root project can be used to define common properties such as:

- dependencies
- plugins
- default configuration for some known plugins
- Java 8 / 17 / LATEST configuration

The LATEST here means the latest JDK.

The project is also configuring Eclipse project using the [eclipse-settings-maven-plugin][1].

## Site

A site can be produced: `mvn site site:stage`

However, the project must first be packaged in a separate step.

## Generating a new version

### Setup

New version are manually created (no automated step, even if attempt were made as in `v29` with [GitHub Actions and Maven releases][4]):

1. Create a GPG key if needed: see [Managing commit signature verification][3]
2. Configure your maven settings:
   1. The `publish.directory.releases` and `publish.directory.snapshots` must target a local directory, and use the `file://` URI pattern.
   2. The `gpg.keyname` and `gpg.passphraseServerId` must be defined.

This should give this `settings.xml`:

```xml
<settings>
  <servers>
    <server> <id>gpg-github</id> <passphrase>foobar</passphrase> </server>
  </servers>

  <profiles>
    <profile>
      <id>project-settings</id>
      <properties>
        <publish.directory>file:///e:/git/github/glhez-maven-repository</publish.directory>
        <publish.directory.releases>${publish.directory}/releases</publish.directory.releases>
        <publish.directory.snapshots>${publish.directory}/snapshots</publish.directory.snapshots>
        <publish.directory.site>${publish.directory}/site</publish.directory.site>
      </properties>
    </profile>
    <profile>
      <id>gpg-github</id>
      <properties>
        <gpg.keyname>YOUR_KEY</gpg.keyname>
        <gpg.passphraseServerId>gpg-github</gpg.passphraseServerId>
      </properties>
    </profile>
  </profiles>

  <activeProfiles>
    <activeProfile>project-settings</activeProfile>
    <activeProfile>gpg-github</activeProfile>
  </activeProfiles>
</settings>
```

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

