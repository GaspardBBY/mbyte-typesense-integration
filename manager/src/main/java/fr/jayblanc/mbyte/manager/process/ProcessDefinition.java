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

import fr.jayblanc.mbyte.manager.core.entity.Environment;
import fr.jayblanc.mbyte.manager.core.entity.EnvironmentEntry;
import fr.jayblanc.mbyte.manager.process.entity.ProcessContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Jerome Blanchard
 */
public class ProcessDefinition {

    private final String name;
    private final String appId;
    private final List<String> tasks;
    private final ProcessContext context;

    public ProcessDefinition(String name, String appId, List<String> tasks, ProcessContext context) {
        this.name = name;
        this.appId = appId;
        this.tasks = tasks;
        this.context = context;
    }

    public String getName() {
        return name;
    }

    public String getAppId() {
        return appId;
    }

    public List<String> getTasks() {
        return tasks;
    }

    public ProcessContext getContext() {
        return context;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name = "unnamed-process";
        private String appId = "";
        private final List<String> tasks = new ArrayList<>();
        private final ProcessContext context = new ProcessContext();

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withAppId(String appId) {
            this.appId = appId;
            return this;
        }

        public Builder withEnvironment(Environment env) {
            for (EnvironmentEntry entry : env.listEntries()) {
                // TODO Include a 'secret' scope to avoid logging secret values
                 this.context.setValue(entry.getKey(), entry.getValue());
            }
            return this;
        }

        public Builder withGlobalContext(Map<String, Serializable> ctx) {
            for (Map.Entry<String, Serializable> entry : ctx.entrySet()) {
                this.context.setValue(entry.getKey(), entry.getValue());
            }
            return this;
        }

        public Builder addTask(String task, Map<String, Serializable> taskContext) {
            String taskName = String.valueOf(tasks.size() + 1).concat(".").concat(task);
            this.tasks.add(taskName);
            for (Map.Entry<String, Serializable> entry : taskContext.entrySet()) {
                this.context.setValue(taskName, entry.getKey(), entry.getValue());
            }
            return this;
        }

        public Builder addTask(String task) {
            return addTask(task, Map.of());
        }

        public ProcessDefinition build() {
            return new ProcessDefinition(name, appId, tasks, context);
        }
    }

}
