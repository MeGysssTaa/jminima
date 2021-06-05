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

package me.darksidecode.jminima.workflow;

import lombok.NonNull;
import me.darksidecode.jminima.phase.*;

import java.util.*;

public class Workflow {

    private final List<Phase<?, ?>> phases = new ArrayList<>();

    private final Map<Class<?>, EmittedValue<?>> emittedValues = new LinkedHashMap<>();

    @SuppressWarnings ("rawtypes")
    private final Map<Class<? extends Phase>, PhaseExecutionException> phaseErrors = new LinkedHashMap<>();

    private int currentPhase;

    public Workflow phase(@NonNull Phase<?, ?> phase) {
        if (phases.contains(phase))
            throw new IllegalStateException("duplicate phase " + phase.getClass().getName());

        phases.add(phase);
        return this;
    }

    public WorkflowExecutionResult executeAll() {
        if (phases.isEmpty())
            throw new IllegalStateException("no phases to execute");

        while (!hasFullyCompleted() && executeNext())
            currentPhase++;

        if (phaseErrors.isEmpty())
            return WorkflowExecutionResult.FULL_SUCCESS;
        else if (phaseErrors.values().stream().noneMatch(PhaseExecutionException::isFatal))
            return WorkflowExecutionResult.PARTIAL_SUCCESS;
        else
            return WorkflowExecutionResult.FATAL_FAILURE;
    }

    public boolean executeNext() {
        if (hasFullyCompleted())
            throw new IllegalStateException("workflow has already fully completed");

        Phase<?, ?> nextPhase = phases.get(currentPhase);
        Class<?> targetTypeClass = nextPhase.getTargetTypeClass();

        if (targetTypeClass == null)
            throw new IllegalArgumentException(
                    "target type class cannot be null (did you mean Void.class?)");

        EmittedValue<?> target = getLastEmittedValueOfType(targetTypeClass);

        if (nextPhase.getBeforeExecutionWatcher() != null)
            nextPhase.getBeforeExecutionWatcher()
                    .handleNoExcept(target.getValue(), target.getError());

        EmittedValue<?> result = nextPhase.executeNoExcept(target.getValue(), target.getError());

        if (nextPhase.getAfterExecutionWatcher() != null) {
            if (result != null)
                nextPhase.getAfterExecutionWatcher()
                        .handleNoExcept(result.getValue(), result.getError());
            else
                nextPhase.getAfterExecutionWatcher()
                        .handleNoExcept(null, null);
        }

        if (result != null) {
            if (result.getValue() != null)
                emittedValues.put(result.getValue().getClass(), result);

            if (result.getError() != null)
                phaseErrors.put(nextPhase.getClass(), result.getError());
        }

        return result == null || result.getError() == null || !result.getError().isFatal(); // true = continue
    }

    public boolean hasFullyCompleted() {
        return currentPhase >= phases.size();
    }

    public PhaseExecutionException getLastErrorOfPhase(@NonNull Class<Phase<?, ?>> phaseClass) {
        return phaseErrors.get(phaseClass);
    }

    public EmittedValue<?> getLastEmittedValueOfType(@NonNull Class<?> targetType) {
        EmittedValue<?> notEmitted = new EmittedValue<>(new TargetNotEmittedException(targetType));
        EmittedValue<?> target = notEmitted;

        for (Class<?> typeClass : emittedValues.keySet()) {
            if (targetType.isAssignableFrom(typeClass)) { // so that we can get child classes by specifying parent
                if (target == notEmitted)
                    target = emittedValues.get(typeClass);
                else {
                    target = new EmittedValue<>(new PhaseExecutionException(
                            true, "multiple emitted values types match target type class "
                            + targetType.getName() + ": " + target.getClass().getName()
                            + " and " + typeClass.getName() + ", consider using a more concrete target type"));

                    break;
                }
            }
        }

        return target;
    }

    public Collection<PhaseExecutionException> getAllErrorsChronological() {
        return phaseErrors.values();
    }

    public float estimateSuccessPercent() {
        return phases.isEmpty() ? 0.0f : (float) currentPhase / phases.size() * 100.0f;
    }
    
}
