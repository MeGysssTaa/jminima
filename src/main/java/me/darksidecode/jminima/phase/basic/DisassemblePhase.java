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
import me.darksidecode.jminima.JMinima;
import me.darksidecode.jminima.disassembling.JavaDisassembler;
import me.darksidecode.jminima.phase.EmittedValue;
import me.darksidecode.jminima.phase.Phase;
import me.darksidecode.jminima.phase.PhaseExecutionException;
import me.darksidecode.jminima.util.JarFileData;
import org.objectweb.asm.tree.ClassNode;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;

public class DisassemblePhase extends Phase<JarFile, JarFileData> {

    private static final String DEFAULT_ERR_MSG_HEADER
            = "the following errors occurred during jar disassembling:";

    private final Constructor<? extends JavaDisassembler> disassemblerConstructor;

    public DisassemblePhase(@NonNull Class<? extends JavaDisassembler> disassemblerClass) {
        try {
            disassemblerConstructor = disassemblerClass.getConstructor(JarFile.class);
        } catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException(
                    "invalid JarDisassembler: missing single-argument " +
                            "constructor with parameter of type JarFile", ex);
        }
    }

    @Override
    public Class<? super JarFile> getTargetTypeClass() {
        return JarFile.class;
    }

    @Override
    protected EmittedValue<? extends JarFileData> execute(JarFile target,
                                                          PhaseExecutionException error) throws Throwable {
        if (target == null)
            return new EmittedValue<>(new PhaseExecutionException(
                    true, "failed to disassemble the target jar file", error));

        StringBuilder errMsgBuilder = new StringBuilder(DEFAULT_ERR_MSG_HEADER);
        Map<ClassNode, Boolean> classes = new HashMap<>();
        boolean anySuccess = disassemble(target, errMsgBuilder, classes);
        String errMsg = errMsgBuilder.toString();
        JarFileData jarFileData = new JarFileData(target, classes, null);

        if (errMsg.equals(DEFAULT_ERR_MSG_HEADER))
            return new EmittedValue<>(jarFileData); // full success
        else
            return new EmittedValue<>(jarFileData,
                   new PhaseExecutionException(!anySuccess, errMsg)); // error(s)
    }

    private boolean disassemble(JarFile jarFile, StringBuilder errMsgBuilder,
                                Map<ClassNode, Boolean> classes) {
        JavaDisassembler disassembler;

        try {
            disassembler = disassemblerConstructor.newInstance(jarFile);
        } catch (ReflectiveOperationException ex) {
            if (JMinima.debug) ex.printStackTrace();
            errMsgBuilder.append("\n    - ").append(ex);
            return false; // fatal error
        }

        jarFile.stream().forEach(entry -> {
            EmittedValue<? extends ClassNode> cls = disassembler.disassemble(entry);

            if (cls != null) {
                if (cls.getError() != null) {
                    if (JMinima.debug) cls.getError().printStackTrace();
                    errMsgBuilder.append("\n    - ").append(cls.getError());
                } else if (cls.getValue().name.equals("java/lang/Object") || cls.getValue().superName != null)
                    classes.put(cls.getValue(), false);
            }
        });

        return true; // full or partial success
    }

}
