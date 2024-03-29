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
import me.darksidecode.jminima.JMinima;
import me.darksidecode.jminima.phase.EmittedValue;
import me.darksidecode.jminima.phase.Phase;
import me.darksidecode.jminima.phase.PhaseExecutionException;
import me.darksidecode.jminima.phase.TargetNotEmittedException;

import java.io.Closeable;
import java.util.*;

public class Workflow implements Closeable {

    //////////////////////////////////////////////////////////////////////////////////////////////////////
    public static final int CLEAR_PHASE_ERRORS        = 0b1                                              ;
    public static final int CLEAR_EMITTED_VALUES      = 0b10                                             ;
    public static final int CLEAR_STATE               = 0b100                                            ;
    //////////////////////////////////////////////////////////////////////////////////////////////////////
    public static final int CLEAR_EXECUTION_ARTIFACTS = CLEAR_PHASE_ERRORS        | CLEAR_EMITTED_VALUES ;
    public static final int CLEAR_ALL                 = CLEAR_EXECUTION_ARTIFACTS | CLEAR_STATE          ;
    //////////////////////////////////////////////////////////////////////////////////////////////////////

    private final int clearOnCloseFlags;

    public Workflow() {
        this(0);
    }

    public Workflow(int clearOnCloseFlags) {
        this.clearOnCloseFlags = clearOnCloseFlags;
    }

    private final List<Phase<?, ?>> phases = new ArrayList<>();

    private final Map<Class<?>, EmittedValue<?>> emittedValues = new LinkedHashMap<>();

    @SuppressWarnings ("rawtypes")
    private final Map<Class<? extends Phase>, PhaseExecutionException> phaseErrors = new LinkedHashMap<>();

    private int currentPhase;

    @Override
    public void close() {
        emittedValues.values().forEach(this::close);

        if ((clearOnCloseFlags & CLEAR_PHASE_ERRORS) != 0)
            clearPhaseErrors();

        if ((clearOnCloseFlags & CLEAR_EMITTED_VALUES) != 0)
            clearEmittedValues();

        if ((clearOnCloseFlags & CLEAR_STATE) != 0)
            clearState();
    }
    
    public void close(@NonNull EmittedValue<?> val) {
        Object value = val.getValue();
        
        if (value instanceof EmittedValue<?>)
            close((EmittedValue<?>) value); // EmittedValue objects can be nested
        else if (value instanceof Closeable) {
            try {
                ((Closeable) value).close();
            } catch (Throwable ignored) {}
        } else if (value instanceof AutoCloseable) { // AutoCloseable is a superclass of Closeable
            try {
                ((AutoCloseable) value).close();
            } catch (Throwable ignored) {}
        }
    }

    public Workflow clearAll() {
        return clearExecutionArtifacts().clearState();
    }

    public Workflow clearExecutionArtifacts() {
        return clearEmittedValues().clearPhaseErrors();
    }

    public Workflow clearState() {
        currentPhase = 0;
        phases.clear();
        return this;
    }

    public Workflow clearEmittedValues() {
        emittedValues.clear();
        return this;
    }

    public Workflow clearPhaseErrors() {
        phaseErrors.clear();
        return this;
    }

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
        EmittedValue<?> result;

        try {
            nextPhase.getWatcher().beforeExecution(target.getValue(), target.getError());
            result = nextPhase.executeNoExcept(target.getValue(), target.getError());

            try {
                if (result != null)
                    nextPhase.getWatcher().afterExecution(result.getValue(), result.getError());
                else
                    nextPhase.getWatcher().afterExecution(null, null);
            } catch (Throwable t) {
                if (JMinima.debug) t.printStackTrace();
                result = new EmittedValue<>(new PhaseExecutionException(
                        true, "fatal unhandled exception in phase execution watcher (afterExecution)", t));
            }
        } catch (Throwable t) {
            if (JMinima.debug) t.printStackTrace();
            result = new EmittedValue<>(new PhaseExecutionException(
                    true, "fatal unhandled exception in phase execution watcher (beforeExecution)", t));
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
