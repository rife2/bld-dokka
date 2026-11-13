# [bld](https://rife2.com/bld) Extension to Generate API Documentation with [Dokka](https://github.com/Kotlin/dokka) for [Kotlin](https://kotlinlang.org/)

[![License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/java-17%2B-blue)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![bld](https://img.shields.io/badge/3.0.1-FA9052?label=bld&labelColor=2392FF)](https://rife2.com/bld)
[![Release](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo.rife2.com%2Freleases%2Fcom%2Fuwyn%2Frife2%2Fbld-dokka%2Fmaven-metadata.xml&color=blue)](https://repo.rife2.com/#/releases/com/uwyn/rife2/bld-dokka)
[![Snapshot](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo.rife2.com%2Fsnapshots%2Fcom%2Fuwyn%2Frife2%2Fbld-dokka%2Fmaven-metadata.xml&label=snapshot)](https://repo.rife2.com/#/snapshots/com/uwyn/rife2/bld-dokka)
[![GitHub CI](https://github.com/rife2/bld-dokka/actions/workflows/bld.yml/badge.svg)](https://github.com/rife2/bld-dokka/actions/workflows/bld.yml)

To install the latest version, add the following to the `lib/bld/bld-wrapper.properties` file:

```properties
bld.extension-dokka=com.uwyn.rife2:bld-dokka
```

For more information, please refer to the [extensions](https://github.com/rife2/bld/wiki/Extensions) documentation.


## Generate API Documentation

To generate a project's documentation in various formats. The examples below use
[`fromProject`](https://rife2.github.io/bld-dokka/rife/bld/extension/DokkaOperation.html#fromProject(rife.bld.BaseProject)),
[`loggingLevel`](https://rife2.github.io/bld-dokka/rife/bld/extension/DokkaOperation.html#loggingLevel(rife.bld.extension.dokka.LoggingLevel)),
[`outputDir`](https://rife2.github.io/bld-dokka/rife/bld/extension/DokkaOperation.html#outputDir(java.io.File)),
[`outputFormat`](https://rife2.github.io/bld-dokka/rife/bld/extension/DokkaOperation.html#outputFormat(rife.bld.extension.dokka.OutputFormat)),
[`failOnWarning`](https://rife2.github.io/bld-dokka/rife/bld/extension/DokkaOperation.html#failOnWarning(boolean)), and
[`execute`](https://rife2.github.io/bld-dokka/rife/bld/extension/DokkaOperation.html#execute()):

```java
import java.io.File;

import rife.bld.extension.DokkaOperation;
import rife.bld.extension.dokka.LoggingLevel;
import rife.bld.extension.dokka.OutputFormat;
import rife.bld.extension.tools.IOTools;

...

@BuildCommand(value = "dokka-gfm", summary = "Generates documentation in GitHub flavored markdown format")
public void dokkaGfm() throws ExitStatusException, IOException, InterruptedException {
    new DokkaOperation()
            .fromProject(this)
            .loggingLevel(LoggingLevel.INFO)
            // Create build/dokka/gfm
            .outputDir(IOTools.resolveFile(buildDirectory(), "dokka", "gfm"))
            .outputFormat(OutputFormat.MARKDOWN) // required
            .execute();
}

@BuildCommand(value = "dokka-html", summary = "Generates documentation in HTML format")
public void dokkaHtml() throws ExitStatusException, IOException, InterruptedException {
    new DokkaOperation()
            .fromProject(this) // required
            .loggingLevel(LoggingLevel.INFO)
            // Create build/dokka/html
            .outputDir(IOTools.resolveFile(buildDirectory(), "dokka", "html"))
            .outputFormat(OutputFormat.HTML) // required
            .execute();
}

@BuildCommand(value = "dokka-jekyll", summary = "Generates documentation in Jekyll flavored markdown format")
public void dokkaJekyll() throws ExitStatusException, IOException, InterruptedException {
    new DokkaOperation()
            .fromProject(this) // required
            .loggingLevel(LoggingLevel.INFO)
            // Create build/dokka/jekyll
            .outputDir(IOTools.resolveFile(buildDirectory(), "dokka", "jekyll"))
            .outputFormat(OutputFormat.JEKYLL) // required
            .execute();
}

@BuildCommand(summary = "Generates Javadoc for the project")
@Override
public void javadoc() throws ExitStatusException, IOException, InterruptedException {
    new DokkaOperation()
            .fromProject(this) // required
            .failOnWarning(true)
            .loggingLevel(LoggingLevel.INFO)
            // Create build/javadoc
            .outputDir(new File(buildDirectory(), "javadoc"))
            .outputFormat(OutputFormat.JAVADOC) // required
            .execute();
}
```

```console
./bld javadoc
./bld dokka-html
./bld dokka-gfm
./bld dokka-jekyll
```

- [View Examples Project](https://github.com/rife2/bld-dokka/tree/main/examples/)

## Configuring Source Sets

[`fromProject`](https://rife2.github.io/bld-dokka/rife/bld/extension/DokkaOperation.html#fromProject(rife.bld.BaseProject)) seeds the source set with `src/main/kotlin`, plus the project's compile and provided classpath jars, the project's `javaRelease` (as the source set's JDK version), and the project name as the module name. Projects with sources elsewhere — for example mixed Java/Kotlin, generated sources, or extra source roots — must extend the source set themselves.

Call [`fromProject`](https://rife2.github.io/bld-dokka/rife/bld/extension/DokkaOperation.html#fromProject(rife.bld.BaseProject)) first so the defaults and classpath are populated, then add sources with [`sourceSet(Consumer<SourceSet)`](https://rife2.github.io/bld-dokka/rife/bld/extension/DokkaOperation.html#sourceSet(java.util.function.Consumer)):

```java
new DokkaOperation()
        .fromProject(this) // seeds src/main/kotlin, classpath, jdkVersion, moduleName
        .sourceSet(ss -> ss
        .src(new File("src/main/java"))      // mixed Java/Kotlin
        .src(new File("src/generated/kotlin")))
        .outputFormat(OutputFormat.JAVADOC)
        .outputDir(new File(buildDirectory(), "javadoc"))
        .execute();
```

For a fully custom source set (no `src/main/kotlin`, no project classpath), pass a configured `SourceSet` to [`sourceSet(SourceSet)`](https://rife2.github.io/bld-dokka/rife/bld/extension/DokkaOperation.html#sourceSet(rife.bld.extension.dokka.SourceSet)) **before** calling [`fromProject`](https://rife2.github.io/bld-dokka/rife/bld/extension/DokkaOperation.html#fromProject(rife.bld.BaseProject)) — [`fromProject`](https://rife2.github.io/bld-dokka/rife/bld/extension/DokkaOperation.html#fromProject(rife.bld.BaseProject)) only adds defaults when the source set is empty, so anything you set first is preserved.

## Setting the Dokka Version

By default, the extension downloads the version defined by the
[`DOKKA_VERSION`](https://rife2.github.io/bld-dokka/rife/bld/extension/DokkaOperation.html#DOKKA_VERSION)
constant.

You can override this to target a specific Dokka release with the [`version(String)`](https://rife2.github.io/bld-dokka/rife/bld/extension/DokkaOperation.html#version(java.lang.String)) method
(or [`version(Version)`](https://rife2.github.io/bld-dokka/rife/bld/extension/DokkaOperation.html#version(rife.bld.dependencies.Version))):

```java
new DokkaOperation()
        .fromProject(this)
        .version("2.1.0") // String form
//      .version(new VersionNumber(2, 1, 0)) // Version form
        .loggingLevel(LoggingLevel.INFO)
        .outputDir(new File(buildDirectory(), "javadoc"))
        .outputFormat(OutputFormat.JAVADOC)
        .execute();
```

> **Warning:** Only Dokka `2.x` releases are supported.

## Other Options

The following are commonly used but not shown in the main examples; see the [API Reference](#api-reference) for the full list.

- [`json(...)`](https://rife2.github.io/bld-dokka/rife/bld/extension/DokkaOperation.html#json(java.lang.String)) — supply a Dokka JSON configuration file instead of (or alongside) the fluent options.
- [`offlineMode(boolean)`](https://rife2.github.io/bld-dokka/rife/bld/extension/DokkaOperation.html#offlineMode(boolean)) — resolve remote links/packages from local caches only.
- [`failOnWarning(boolean)`](https://rife2.github.io/bld-dokka/rife/bld/extension/DokkaOperation.html#failOnWarning(boolean)) — fail the build if Dokka emits a warning or error (used in the `javadoc` example above).
- [`pluginConfigurations(String, String)`](https://rife2.github.io/bld-dokka/rife/bld/extension/DokkaOperation.html#pluginConfigurations(java.lang.String,java.lang.String)) /
  [`pluginsClasspath(File...)`](https://rife2.github.io/bld-dokka/rife/bld/extension/DokkaOperation.html#pluginsClasspath(java.io.File...)) — configure Dokka plugins and the jars they need.

## Template Project

There is also a [Kotlin Template Project](https://github.com/rife2/kotlin-bld-example) with support for Dokka and the
[Detekt](https://github.com/rife2/bld-detekt) extensions.

## API Reference

Please check the [Dokka Operation documentation](https://rife2.github.io/bld-dokka/rife/bld/extension/DokkaOperation.html#method-summary)
for all available configuration options.