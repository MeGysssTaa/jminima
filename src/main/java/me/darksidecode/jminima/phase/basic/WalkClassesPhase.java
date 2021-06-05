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
import me.darksidecode.jminima.util.JarFileData;
import me.darksidecode.jminima.walking.ClassWalker;
import org.objectweb.asm.tree.ClassNode;

import java.lang.reflect.Constructor;

public class WalkClassesPhase extends Phase<JarFileData, Void> {

    private static final String DEFAULT_ERR_MSG_HEADER
            = "the following errors occurred during classes walking:";

    private final Constructor<? extends ClassWalker> walkerConstructor;

    public WalkClassesPhase(@NonNull Class<? extends ClassWalker> walkerClass) {
        try {
            walkerConstructor = walkerClass.getConstructor(ClassNode.class);
        } catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException(
                    "invalid ClassWalker: missing single-argument " +
                            "constructor with parameter of type ClassNode", ex);
        }
    }

    @Override
    public Class<? super JarFileData> getTargetTypeClass() {
        return JarFileData.class;
    }

    @Override
    protected EmittedValue<? extends Void> execute(JarFileData target,
                                                   PhaseExecutionException error) throws Exception {
        if (target == null || target.getJarFile() == null || target.getClasses() == null)
            return new EmittedValue<>(new PhaseExecutionException(
                    true, "failed to walk classes of the target disassembled data", error));

        StringBuilder errMsgBuilder = new StringBuilder(DEFAULT_ERR_MSG_HEADER);
        boolean anySuccess = walkClasses(target, errMsgBuilder);
        String errMsg = errMsgBuilder.toString();

        if (errMsg.equals(DEFAULT_ERR_MSG_HEADER))
            return null; // full success
        else
            return new EmittedValue<>(
                   new PhaseExecutionException(!anySuccess, errMsg)); // error(s)
    }

    private boolean walkClasses(JarFileData jarFileData, StringBuilder errMsgBuilder) {
        for (ClassNode cls : jarFileData.getClasses().keySet()) {
            try {
                ClassWalker walker = walkerConstructor.newInstance(cls);

                walker.visitClass();

                if (cls.fields != null)
                    cls.fields.forEach(walker::visitField);

                if (cls.methods != null)
                    cls.methods.forEach(walker::visitMethod);

                if (walker.hasModifiedAnything())
                    jarFileData.getClasses().put(cls, true);
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
