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

import org.junit.jupiter.api.Test;
import rife.bld.blueprints.BaseProjectBlueprint;
import rife.bld.extension.dokka.LoggingLevel;
import rife.bld.extension.dokka.OutputFormat;
import rife.bld.extension.dokka.SourceSet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class DokkaOperationTest {
    @Test
    @SuppressWarnings({"PMD.AvoidDuplicateLiterals"})
    void executeConstructProcessCommandListTest() throws IOException {
        var args = Files.readAllLines(Paths.get("src", "test", "resources", "dokka-args.txt"));

        assertThat(args).isNotEmpty();

        var examples = new File("examples");
        var jsonConf = new File("config.json");
        var op = new DokkaOperation()
                .delayTemplateSubstitution(true)
                .failOnWarning(true)
                .fromProject(new BaseProjectBlueprint(examples, "com.example", "Example"))
                .globalLinks("s", "link")
                .globalLinks(Map.of("s2", "link2"))
                .globalPackageOptions("option1", "option2")
                .globalPackageOptions(List.of("option3", "option4"))
                .globalSrcLink("link1", "link2")
                .globalSrcLink(List.of("link3", "link4"))
                .includes(new File("file1"))
                .includes("file2")
                .includes(List.of(new File("file3"), new File("file4")))
                .json(jsonConf)
                .loggingLevel(LoggingLevel.DEBUG)
                .moduleName("name")
                .moduleVersion("1.0")
                .noSuppressObviousFunctions(true)
                .offlineMode(true)
                .outputDir(new File(examples, "build"))
                .outputFormat(OutputFormat.JAVADOC)
                .pluginConfigurations("name", "{\"json\"}")
                .pluginConfigurations(Map.of("{\"name2\"}", "json2", "name3}", "{json3"))
                .pluginsClasspath(new File("path1"))
                .pluginsClasspath("path2")
                .pluginsClasspath(List.of(new File("path3"), new File("path4")))
                .sourceSet(new SourceSet().classpath(
                        List.of(
                                new File("examples/foo.jar"),
                                new File("examples/bar.jar")
                        )))
                .suppressInheritedMembers(true);

        assertThat(op.globalLinks()).as("globalLinks").hasSize(2);
        assertThat(op.globalPackageOptions()).as("globalPackageOptions").hasSize(4);
        assertThat(op.globalSrcLink()).as("globalSrcLink").hasSize(4);
        assertThat(op.includes()).as("includes").hasSize(4);
        assertThat(op.pluginConfigurations()).as("pluginConfigurations").hasSize(3);
        assertThat(op.pluginsClasspath()).as("pluginsClasspath").hasSize(9);

        var params = op.executeConstructProcessCommandList();
        for (var p : args) {
            var found = false;
            for (var a : params) {
                if (a.startsWith(p)) {
                    found = true;
                    break;
                }
            }
            assertThat(found).as(p + " not found.").isTrue();
        }

        var path = examples.getAbsolutePath();
        var dokkaJar = "1.9.20.jar";
        var matches = List.of("java",
                "-jar", path + "/lib/bld/dokka-cli-" + dokkaJar,
                "-pluginsClasspath", path + "/lib/bld/dokka-base-" + dokkaJar + ';' +
                        path + "/lib/bld/analysis-kotlin-descriptors-" + dokkaJar + ';' +
                        path + "/lib/bld/javadoc-plugin-" + dokkaJar + ';' +
                        path + "/lib/bld/korte-jvm-4.0.10.jar;" +
                        path + "/lib/bld/kotlin-as-java-plugin-" + dokkaJar + ';' +
                        TestUtil.localPath("path1", "path2", "path3", "path4"),
                "-sourceSet", "-src " + path + "/src/main/kotlin" + " -classpath " + path + "/foo.jar;"
                        + path + "/bar.jar",
                "-outputDir", path + "/build",
                "-delayTemplateSubstitution",
                "-failOnWarning",
                "-globalLinks", "s^link^^s2^link2",
                "-globalPackageOptions", "option1;option2;option3;option4",
                "-globalSrcLinks_", "link1;link2;link3;link4",
                "-includes", TestUtil.localPath("file1", "file2", "file3", "file4"),
                "-loggingLevel", "debug",
                "-moduleName", "name",
                "-moduleVersion", "1.0",
                "-noSuppressObviousFunctions",
                "-offlineMode",
                "-pluginsConfiguration", "{\\\"name2\\\"}={json2}^^{name}={\\\"json\\\"}^^{name3}}={{json3}",
                "-suppressInheritedMembers",
                jsonConf.getAbsolutePath());

        assertThat(params).hasSize(matches.size());

        IntStream.range(0, params.size()).forEach(i -> {
            if (params.get(i).contains(".jar;")) {
                var jars = params.get(i).split(";");
                Arrays.stream(jars).forEach(jar -> assertThat(matches.get(i)).as(matches.get(i)).contains(jar));
            } else {
                assertThat(params.get(i)).as(params.get(i)).isEqualTo(matches.get(i));
            }
        });
    }
}
