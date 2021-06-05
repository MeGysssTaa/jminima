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

package me.darksidecode.jminima.phase;

import lombok.Getter;

public abstract class Phase<TargetType, EmitType> {

    @Getter
    private PhaseExecutionWatcher<EmitType> beforeExecutionWatcher, afterExecutionWatcher;

    public final Phase<TargetType, EmitType> beforeExecution(PhaseExecutionWatcher<EmitType> watcher) {
        this.beforeExecutionWatcher = watcher;
        return this;
    }

    public final Phase<TargetType, EmitType> afterExecution(PhaseExecutionWatcher<EmitType> watcher) {
        this.afterExecutionWatcher = watcher;
        return this;
    }

    public final EmittedValue<? extends EmitType> executeNoExcept(Object target, PhaseExecutionException error) {
        try {
            if (target == null && error == null)
                throw new IllegalArgumentException("target and error must not be null at the same time");

            if (target != null && !getTargetTypeClass().isAssignableFrom(target.getClass()))
                throw new IllegalArgumentException("invalid target type " + target.getClass().getName()
                        + " (expected " + getTargetTypeClass().getName() + " or its inheritors)");

            return target != null ? execute((TargetType) target, error) : execute(null, error);
        } catch (Exception ex) {
            return new EmittedValue<>(new PhaseExecutionException(
                    true, "unhandled exception during phase execution", ex));
        }
    }

    public abstract Class<? super TargetType> getTargetTypeClass();

    protected abstract EmittedValue<? extends EmitType> execute(
            TargetType target, PhaseExecutionException error) throws Exception;

}
