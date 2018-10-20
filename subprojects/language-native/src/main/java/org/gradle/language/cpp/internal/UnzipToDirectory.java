/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.language.cpp.internal;

import com.google.common.io.Files;
import org.apache.commons.io.IOUtils;
import org.gradle.api.UncheckedIOException;
import org.gradle.api.artifacts.transform.ArtifactTransform;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UnzipToDirectory extends ArtifactTransform {
        @Override
        public List<File> transform(File headersZip) {
            try {
                unzipTo(headersZip);
            } catch (IOException e) {
                throw new UncheckedIOException("extracting header zip", e);
            }
            return Collections.singletonList(getOutputDirectory());
        }

        // TODO: Find a better reusable unzip infrastructure
        private void unzipTo(File headersZip) throws IOException {
            ZipInputStream inputStream = new ZipInputStream(new BufferedInputStream(new FileInputStream(headersZip)));
            try {
                ZipEntry entry;
                while ((entry = inputStream.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        continue;
                    }
                    File outFile = new File(getOutputDirectory(), entry.getName());
                    Files.createParentDirs(outFile);
                    FileOutputStream outputStream = new FileOutputStream(outFile);
                    try {
                        IOUtils.copyLarge(inputStream, outputStream);
                    } finally {
                        outputStream.close();
                    }
                }
            } finally {
                inputStream.close();
            }
        }
    }