# [bld](https://rife2.com/bld) Extension to Generate API Documentation with [Dokka](https://github.com/Kotlin/dokka) for [Kotlin](https://kotlinlang.org/)

[![License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/java-17%2B-blue)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![bld](https://img.shields.io/badge/2.3.0-FA9052?label=bld&labelColor=2392FF)](https://rife2.com/bld)
[![Release](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo.rife2.com%2Freleases%2Fcom%2Fuwyn%2Frife2%2Fbld-dokka%2Fmaven-metadata.xml&color=blue)](https://repo.rife2.com/#/releases/com/uwyn/rife2/bld-dokka)
[![Snapshot](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo.rife2.com%2Fsnapshots%2Fcom%2Fuwyn%2Frife2%2Fbld-dokka%2Fmaven-metadata.xml&label=snapshot)](https://repo.rife2.com/#/snapshots/com/uwyn/rife2/bld-dokka)
[![GitHub CI](https://github.com/rife2/bld-dokka/actions/workflows/bld.yml/badge.svg)](https://github.com/rife2/bld-dokka/actions/workflows/bld.yml)

To install the latest version, add the following to the `lib/bld/bld-wrapper.properties` file:

```properties
bld.extension-dokka=com.uwyn.rife2:bld-dokka
```

For more information, please refer to the [extensions](https://github.com/rife2/bld/wiki/Extensions) documentation.


## Generate API Documentation

To generate a project's documentation in various formats:

```java
@BuildCommand(value = "dokka-gfm", summary = "Generates documentation in GitHub flavored markdown format")
public void dokkaGfm() throws ExitStatusException, IOException, InterruptedException {
    new DokkaOperation()
            .fromProject(this)
            .loggingLevel(LoggingLevel.INFO)
            // Create build/dokka/gfm 
            .outputDir(Path.of(buildDirectory().getAbsolutePath(), "dokka", "gfm").toFile())
            .outputFormat(OutputFormat.MARKDOWN)
            .execute();
}

@BuildCommand(value = "dokka-html", summary = "Generates documentation in HTML format")
public void dokkaHtml() throws ExitStatusException, IOException, InterruptedException {
    new DokkaOperation()
            .fromProject(this)
            .loggingLevel(LoggingLevel.INFO)
            // Create build/dokka/html
            .outputDir(Path.of(buildDirectory().getAbsolutePath(), "dokka", "html").toFile())
            .outputFormat(OutputFormat.HTML)
            .execute();
}

@BuildCommand(value = "dokka-jekyll", summary = "Generates documentation in Jekyll flavored markdown format")
public void dokkaJekyll() throws ExitStatusException, IOException, InterruptedException {
    new DokkaOperation()
            .fromProject(this)
            .loggingLevel(LoggingLevel.INFO)
            // Create build/dokka/jekyll
            .outputDir(Path.of(buildDirectory().getAbsolutePath(), "dokka", "jekkyl").toFile())
            .outputFormat(OutputFormat.JEKYLL)
            .execute();
}

@BuildCommand(summary = "Generates Javadoc for the project")
@Override
public void javadoc() throws ExitStatusException, IOException, InterruptedException {
    new DokkaOperation()
            .fromProject(this)
            .failOnWarning(true)
            .loggingLevel(LoggingLevel.INFO)
            // Create build/javadoc
            .outputDir(new File(buildDirectory(), "javadoc"))
            .outputFormat(OutputFormat.JAVADOC)
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

Please check the [Dokka Operation documentation](https://rife2.github.io/bld-dokka/rife/bld/extension/DokkaOperation.html#method-summary)
for all available configuration options.

## Template Project

There is also a [Kotlin Template Project](https://github.com/rife2/kotlin-bld-example) with support for Dokka and the
[Detekt](https://github.com/rife2/bld-detekt) extensions.
