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
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;

public class InjectClassesPhase extends InjectJarEntriesPhase {

    public InjectClassesPhase(@NonNull Collection<? extends ClassNode> classes) {
        super(fromClassNodes(classes));
    }

    public InjectClassesPhase(@NonNull Map<String, ? extends ClassWriter> classes) {
        super(fromClassWriters(classes));
    }

    private static Map<JarEntry, byte[]> fromClassNodes(@NonNull Collection<? extends ClassNode> classes) {
        Map<String, ClassWriter> map = new HashMap<>();

        for (ClassNode cls : classes) {
            ClassWriter writer = new ClassWriter(0);
            cls.accept(writer);
            map.put(cls.name, writer);
        }

        return fromClassWriters(map);
    }

    private static Map<JarEntry, byte[]> fromClassWriters(@NonNull Map<String, ? extends ClassWriter> classes) {
        Map<JarEntry, byte[]> map = new HashMap<>();

        for (String className : classes.keySet())
            map.put(
                    new JarEntry(className + ".class"),
                    classes.get(className).toByteArray()
            );

        return map;
    }

}
