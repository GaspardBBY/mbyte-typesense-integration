/*
 * Copyright (C) 2025 Jerome Blanchard <jayblanc@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package fr.jayblanc.mbyte.manager.process;

import fr.jayblanc.mbyte.manager.process.entity.ProcessContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Jerome Blanchard
 */
public class ProcessDefinition {

    private final String store;
    private final String name;
    private final List<String> tasks;
    private final ProcessContext context;

    public ProcessDefinition(String store, String name, List<String> tasks, ProcessContext context) {
        this.store = store;
        this.name = name;
        this.tasks = tasks;
        this.context = context;
    }

    public String getStore() {
        return store;
    }

    public String getName() {
        return name;
    }

    public List<String> getTasks() {
        return tasks;
    }

    public ProcessContext getContext() {
        return context;
    }

    public boolean isParallelRunAllowed() {
        return Boolean.FALSE;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String store = "";
        private String name = "default-unnamed-job";
        private final List<String> tasks = new ArrayList<>();
        private final ProcessContext context = new ProcessContext();

        public Builder forStore(String store) {
            this.store = store;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder addTask(String task) {
            this.tasks.add(task);
            return this;
        }

        public Builder setTaskContextEntry(String key, Serializable value) {
            if (tasks.isEmpty()) {
                throw new IllegalStateException("Cannot add task context entry when no task has been added yet");
            }
            this.context.setValue(ProcessContext.TASK_SCOPE_PREFIX.concat(Integer.toString(tasks.size())), key, value);
            return this;
        }

        public Builder setGlobalContextEntry(String key, Serializable value) {
            this.context.setValue(key, value);
            return this;
        }

        public ProcessDefinition build() {
            return new ProcessDefinition(store, name, tasks, context);
        }
    }

}
