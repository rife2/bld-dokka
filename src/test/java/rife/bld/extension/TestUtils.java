/*
 * Copyright 2023-2026 the original author or authors.
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

import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

import static rife.bld.extension.DokkaOperation.SEMICOLON;

@SuppressWarnings("PMD.TestClassWithoutTestCases")
public final class TestUtils {
    private TestUtils() {
        // no-op
    }

    /**
     * Returns the local path of the given file names.
     *
     * @param fileNames The file names
     * @return the local path
     */
    public static String localPath(String... fileNames) {
        return Arrays.stream(fileNames).map(f -> new File(f).getAbsolutePath()).collect(Collectors.joining(SEMICOLON));
    }
}
