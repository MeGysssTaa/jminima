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

import java.io.File;
import java.util.jar.JarFile;

@RequiredArgsConstructor
public class OpenJarFilePhase extends Phase<Void, JarFile> {

    @NonNull
    private final File file;

    @Override
    public Class<? super Void> getTargetTypeClass() {
        return Void.class;
    }

    @Override
    protected EmittedValue<? extends JarFile> execute(Void target,
                                                      PhaseExecutionException error) throws Exception {
        if (!file.isFile())
            return new EmittedValue<>(new PhaseExecutionException(
                    true, "target file does not exist or is a directory"));

        if (!file.canRead())
            return new EmittedValue<>(new PhaseExecutionException(
                    true, "target file cannot be read from"));

        JarFile jarFile = new JarFile(file);

        return file.canWrite()
                ? new EmittedValue<>(jarFile)
                : new EmittedValue<>(jarFile, new PhaseExecutionException(
                        false, "target file cannot be written to"));
    }

}
