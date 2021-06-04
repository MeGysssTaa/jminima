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

import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;

@RequiredArgsConstructor
public class InjectJarEntriesPhase extends Phase<JarFileData, Void> {

    @NonNull
    private final Map<JarEntry, byte[]> jarEntries;

    @Override
    public Class<? super JarFileData> getTargetTypeClass() {
        return JarFileData.class;
    }

    @Override
    protected EmittedValue<? extends Void> execute(JarFileData target,
                                                   PhaseExecutionException error) throws Exception {
        if (target == null)
            return new EmittedValue<>(new PhaseExecutionException(
                    true, "failed to inject an entry in the target jar file", error));

        if (target.getInjectedEntries() == null)
            target.setInjectedEntries(new HashMap<>());

        target.getInjectedEntries().putAll(jarEntries);

        return null; // success
    }

}
