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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.darksidecode.jminima.JMinima;

import java.util.function.Consumer;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public final class PhaseExecutionWatcher<T> {

    private Watcher<T> beforeExecutionWatcher, afterExecutionWatcher;

    private Consumer<Throwable> errorHandler;

    public PhaseExecutionWatcher<T> beforeExecutionWatcher(Watcher<T> watcher) {
        beforeExecutionWatcher = watcher;
        return this;
    }

    public PhaseExecutionWatcher<T> afterExecutionWatcher(Watcher<T> watcher) {
        afterExecutionWatcher = watcher;
        return this;
    }

    public PhaseExecutionWatcher<T> errorHandler(Consumer<Throwable> handler) {
        errorHandler = handler;
        return this;
    }

    public void beforeExecution(Object object, PhaseExecutionException error) throws Throwable {
        accept(beforeExecutionWatcher, object, error);
    }

    public void afterExecution(Object object, PhaseExecutionException error) throws Throwable {
        accept(afterExecutionWatcher, object, error);
    }

    private void accept(Watcher<T> watcher, Object object, PhaseExecutionException error) throws Throwable {
        if (watcher != null) {
            try {
                watcher.handleBridge(object, error);
            } catch (Throwable t) {
                if (JMinima.debug) t.printStackTrace();
                if (errorHandler != null) errorHandler.accept(t);
            }
        }
    }

    public interface Watcher<T> {
        default void handleBridge(Object object, PhaseExecutionException error) throws Throwable {
            if (object != null) handle((T) object, error);
            else                handle(null, error);
        }

        void handle(T object, PhaseExecutionException error) throws Throwable;
    }

}
