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
import me.darksidecode.jminima.phase.EmittedValue;
import me.darksidecode.jminima.phase.Phase;
import me.darksidecode.jminima.phase.PhaseExecutionException;
import me.darksidecode.jminima.printing.BytecodePrinter;
import me.darksidecode.jminima.util.JarFileData;
import org.objectweb.asm.tree.ClassNode;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PrintClassBytecodePhase extends Phase<JarFileData, Map<? extends ClassNode, String>> {

    private static final String DEFAULT_ERR_MSG_HEADER
            = "the following errors occurred during classes bytecode printing:";

    private final Constructor<? extends BytecodePrinter> printerConstructor;

    public PrintClassBytecodePhase(@NonNull Class<? extends BytecodePrinter> printerClass) {
        try {
            printerConstructor = printerClass.getConstructor(ClassNode.class);
        } catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException(
                    "invalid BytecodePrinter: missing single-argument " +
                            "constructor with parameter of type ClassNode", ex);
        }
    }

    @Override
    public Class<? super JarFileData> getTargetTypeClass() {
        return JarFileData.class;
    }

    @Override
    protected EmittedValue<? extends Map<ClassNode, String>> execute(JarFileData target,
                                                                     PhaseExecutionException error) throws Exception {
        if (target == null)
            return new EmittedValue<>(new PhaseExecutionException(
                    true, "failed to print bytecode of the target disassembled data", error));

        StringBuilder errMsgBuilder = new StringBuilder(DEFAULT_ERR_MSG_HEADER);
        Map<ClassNode, String> map = new HashMap<>();
        boolean anySuccess = printBytecode(target.getClasses().keySet(), errMsgBuilder, map);
        String errMsg = errMsgBuilder.toString();

        if (errMsg.equals(DEFAULT_ERR_MSG_HEADER))
            return new EmittedValue<>(map); // full success
        else
            return new EmittedValue<>(map,
                   new PhaseExecutionException(!anySuccess, errMsg)); // error(s)
    }

    private boolean printBytecode(Collection<? extends ClassNode> classes, StringBuilder errMsgBuilder,
                                  Map<ClassNode, String> map) {
        for (ClassNode cls : classes) {
            try {
                BytecodePrinter printer = printerConstructor.newInstance(cls);
                map.put(cls, printer.getHumanReadableString());
            } catch (ReflectiveOperationException ex) {
                if (JMinima.debug) ex.printStackTrace();
                errMsgBuilder.append("\n    - ").append(ex);
                return false; // fatal error
            } catch (Exception ex) {
                if (JMinima.debug) ex.printStackTrace();
                errMsgBuilder.append("\n    - ").append(ex);
            }
        }

        return true; // full or partial success
    }

}
