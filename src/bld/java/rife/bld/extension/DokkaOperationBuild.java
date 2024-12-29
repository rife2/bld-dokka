/*
 * Copyright 2023-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rife.bld.extension;

import rife.bld.BuildCommand;
import rife.bld.Project;
import rife.bld.publish.PublishDeveloper;
import rife.bld.publish.PublishLicense;
import rife.bld.publish.PublishScm;

import java.util.List;

import static rife.bld.dependencies.Repository.*;
import static rife.bld.dependencies.Scope.compile;
import static rife.bld.dependencies.Scope.test;
import static rife.bld.operations.JavadocOptions.DocLinkOption.NO_MISSING;

public class DokkaOperationBuild extends Project {
    public DokkaOperationBuild() {
        pkg = "rife.bld.extension";
        name = "bld-dokka";
        version = version(1, 0, 2);

        javaRelease = 17;

        downloadSources = true;
        autoDownloadPurge = true;

        repositories = List.of(MAVEN_LOCAL, MAVEN_CENTRAL, RIFE2_RELEASES, RIFE2_SNAPSHOTS);

        var dokka = version(2, 0, 0);
        scope(compile)
                .include(dependency("org.jetbrains.dokka", "dokka-cli", dokka))
                .include(dependency("org.jetbrains.dokka", "dokka-base", dokka))
                .include(dependency("org.jetbrains.dokka", "analysis-kotlin-descriptors", dokka))
                .include(dependency("org.jetbrains.dokka", "javadoc-plugin", dokka))
                .include(dependency("org.jetbrains.dokka", "gfm-plugin", dokka))
                .include(dependency("org.jetbrains.dokka", "jekyll-plugin", dokka))
                .include(dependency("com.uwyn.rife2", "bld", version(2, 1, 0)));
        scope(test)
                .include(dependency("org.junit.jupiter", "junit-jupiter", version(5, 11, 4)))
                .include(dependency("org.junit.platform", "junit-platform-console-standalone", version(1, 11, 4)))
                .include(dependency("org.assertj", "assertj-core", version(3, 27, 0)));

        javadocOperation()
                .javadocOptions()
                .author()
                .docLint(NO_MISSING)
                .link("https://rife2.github.io/bld/");

        publishOperation()
                .repository(version.isSnapshot() ? repository("rife2-snapshot") : repository("rife2"))
                .repository(repository("github"))
                .info()
                .groupId("com.uwyn.rife2")
                .artifactId(name)
                .description("bld Dokka Extension")
                .url("https://github.com/rife2/bld-dokka")
                .developer(new PublishDeveloper()
                        .id("ethauvin")
                        .name("Erik C. Thauvin")
                        .email("erik@thauvin.net")
                        .url("https://erik.thauvin.net/")
                )
                .license(new PublishLicense()
                        .name("The Apache License, Version 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0.txt")
                )
                .scm(new PublishScm()
                        .connection("scm:git:https://github.com/rife2/bld-dokka.git")
                        .developerConnection("scm:git:git@github.com:rife2/bld-dokka.git")
                        .url("https://github.com/rife2/bld-dokka")
                )
                .signKey(property("sign.key"))
                .signPassphrase(property("sign.passphrase"));
    }

    public static void main(String[] args) {
        new DokkaOperationBuild().start(args);
    }

    @BuildCommand(summary = "Runs PMD analysis")
    public void pmd() throws Exception {
        new PmdOperation()
                .fromProject(this)
                .failOnViolation(true)
                .ruleSets("config/pmd.xml")
                .execute();
    }

    @Override
    public void test() throws Exception {
        new ExecOperation()
                .fromProject(this)
                .command("scripts/cliargs.sh")
                .execute();
        super.test();
    }
}
