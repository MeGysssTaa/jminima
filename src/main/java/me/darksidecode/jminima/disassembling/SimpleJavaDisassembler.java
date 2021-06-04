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

package me.darksidecode.jminima.disassembling;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.darksidecode.jminima.phase.EmittedValue;
import me.darksidecode.jminima.phase.PhaseExecutionException;
import me.darksidecode.jminima.util.JarFileData;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@RequiredArgsConstructor
public class SimpleJavaDisassembler implements JavaDisassembler {

    @NonNull
    private final JarFile jarFile;

    @Override
    public EmittedValue<? extends ClassNode> disassemble(@NonNull JarEntry entry) {
        try (InputStream stream = jarFile.getInputStream(entry)) {
            if (JarFileData.isClassEntry(entry)) {
                byte[] bytes = IOUtils.toByteArray(stream);

                if (bytes.length > 4) {
                    String magicNumberHex = String.format(
                            "%02X%02X%02X%02X", bytes[0], bytes[1], bytes[2], bytes[3]);

                    if (magicNumberHex.equalsIgnoreCase("cafebabe"))
                        return disassembleJavaClass(entry.getName(), bytes);
                }
            }
        } catch (Exception ex) {
            return new EmittedValue<>(new PhaseExecutionException(
                    false, "failed to read a jar entry: " + entry.getName(), ex));
        }

        return null; // not a class entry
    }

    private static EmittedValue<? extends ClassNode> disassembleJavaClass(String name, byte[] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        ClassNode cls = new ClassNode();

        try {
            reader.accept(cls, ClassReader.EXPAND_FRAMES);
        } catch (Exception ex1) {
            try {
                reader.accept(cls, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
            } catch (Exception ex2) {
                return new EmittedValue<>(new PhaseExecutionException(
                        false, "failed to disassemble a " + classBytes.length
                        + " bytes Java class: " + name, ex2));
            }
        }

        return new EmittedValue<>(cls);
    }

}
