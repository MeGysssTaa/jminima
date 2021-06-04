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
import lombok.NonNull;

@Getter
public class EmittedValue<E> {

    private final E value;

    private final PhaseExecutionException error;

    public EmittedValue(@NonNull E value) {
        this(value, null);
    }

    public EmittedValue(@NonNull PhaseExecutionException error) {
        this(null, error);
    }

    public EmittedValue(E value, PhaseExecutionException error) {
        this.value = value;
        this.error = error;
    }

}
