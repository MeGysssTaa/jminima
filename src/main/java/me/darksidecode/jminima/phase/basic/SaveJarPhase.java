/*
 * Copyright 2021 German Vekhorev (DarksideCode)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.darksidecode.jminima.phase.basic;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.darksidecode.jminima.phase.EmittedValue;
import me.darksidecode.jminima.phase.Phase;
import me.darksidecode.jminima.phase.PhaseExecutionException;
import me.darksidecode.jminima.util.JarFileData;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

@RequiredArgsConstructor
public class SaveJarPhase extends Phase<JarFileData, Void> {

    @NonNull
    private final File outputFile;

    private final boolean overwrite;

    @Override
    public Class<? super JarFileData> getTargetTypeClass() {
        return JarFileData.class;
    }

    @Override
    protected EmittedValue<? extends Void> execute(JarFileData target,
                                                   PhaseExecutionException error) throws Exception {
        if (target == null)
            return new EmittedValue<>(new PhaseExecutionException(true, "failed to save jar", error));

        if (outputFile.exists()) {
            if (overwrite) {
                if (!outputFile.delete())
                    return new EmittedValue<>(new PhaseExecutionException(
                            true, "output file already exists, and cannot be overwritten (deleted)", error));
            } else
                return new EmittedValue<>(new PhaseExecutionException(
                        true, "output file already exists, and overwrite is set to false", error));
        }

        saveJar(target);

        return null; // success
    }

    private void saveJar(JarFileData jarFileData) throws IOException {
        try (JarOutputStream stream = new JarOutputStream(new FileOutputStream(outputFile))) {
            if (jarFileData.getInjectedEntries() != null) {
                // Add newly created entries - injected files.
                for (JarEntry outputEntry : jarFileData.getInjectedEntries().keySet()) {
                    stream.putNextEntry(outputEntry);
                    stream.write(jarFileData.getInjectedEntries().get(outputEntry));
                    stream.closeEntry();
                }
            }

            if (jarFileData.getJarFile() != null && jarFileData.getClasses() != null) {
                // Process entries that existed before, on read. Modified entries will
                // be overwritten, and non-modified entries will be copied as is.
                Enumeration<JarEntry> entries = jarFileData.getJarFile().entries();

                while (entries.hasMoreElements()) {
                    JarEntry sourceEntry = entries.nextElement();
                    String name = sourceEntry.getName();
                    JarEntry outputEntry = new JarEntry(name);
                    stream.putNextEntry(outputEntry);

                    ClassNode correspondingClass = jarFileData.getClasses().keySet().stream()
                            .filter(cls -> name.equals(cls.name + ".class") || name.equals(cls.name + ".class/"))
                            .findAny().orElse(null);

                    if (correspondingClass != null && jarFileData.getClasses().get(correspondingClass)) {
                        // Serialize the modified ClassNode and overwrite it.
                        ClassWriter writer = new ClassWriter(0);
                        correspondingClass.accept(writer);
                        stream.write(writer.toByteArray());
                    } else
                        // Copy the entry as is.
                        IOUtils.copy(jarFileData.getJarFile().getInputStream(sourceEntry), stream);

                    stream.closeEntry();
                }
            }
        }
    }

}
