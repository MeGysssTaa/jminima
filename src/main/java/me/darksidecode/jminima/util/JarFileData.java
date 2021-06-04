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

package me.darksidecode.jminima.util;

import lombok.*;
import org.objectweb.asm.tree.ClassNode;

import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class JarFileData {

    private JarFile jarFile;

    private Map<ClassNode, Boolean> classes; // true = class has been modified; false = class is as it was on read

    private Map<JarEntry, byte[]> injectedEntries;

    public static boolean isClassEntry(@NonNull JarEntry entry) {
        // It is possible to save classes bytecode inside jar entries with names ending with "/".
        // This makes such classes "invisible" for many decompilers. Some obfuscators abuse this.
        return entry.getName().endsWith(".class") || entry.getName().endsWith(".class/");
    }

}
