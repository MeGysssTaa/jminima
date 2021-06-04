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

import java.util.function.Supplier;

@RequiredArgsConstructor
public class EmitArbitraryValuePhase<EmitType> extends Phase<Void, EmitType> {

    @NonNull
    private final Supplier<? extends EmitType> emitter;

    @Override
    public Class<? super Void> getTargetTypeClass() {
        return Void.class;
    }

    @Override
    protected EmittedValue<? extends EmitType> execute(Void target,
                                                       PhaseExecutionException error) throws Exception {
        return new EmittedValue<>(emitter.get());
    }

}
