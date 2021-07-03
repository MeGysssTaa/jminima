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
import me.darksidecode.jminima.phase.EmittedValue;
import me.darksidecode.jminima.phase.Phase;
import me.darksidecode.jminima.phase.PhaseExecutionException;

import java.util.function.Supplier;

public class EmitArbitraryValuePhase<EmitType> extends Phase<Void, EmitType> {

    private final EmittedValue<? extends EmitType> emitConstant;

    private final Supplier<? extends EmitType> emitter;

    public EmitArbitraryValuePhase(@NonNull EmitType emitConstant) {
        this.emitConstant = new EmittedValue<>(emitConstant);
        this.emitter = null;
    }

    public EmitArbitraryValuePhase(@NonNull Supplier<? extends EmitType> emitter) {
        this.emitConstant = null;
        this.emitter = emitter;
    }

    @Override
    public Class<? super Void> getTargetTypeClass() {
        return Void.class;
    }

    @Override
    protected EmittedValue<? extends EmitType> execute(Void target,
                                                       PhaseExecutionException error) throws Throwable {
        return emitter != null ? new EmittedValue<>(emitter.get()) : emitConstant;
    }

}
