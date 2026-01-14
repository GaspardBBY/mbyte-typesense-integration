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
package fr.jayblanc.mbyte.manager.process.task;

import fr.jayblanc.mbyte.manager.process.entity.ProcessContext;
import org.jobrunr.jobs.lambdas.JobRequest;

/**
 * @author Jerome Blanchard
 */
public class TaskRequest implements JobRequest {

    private String processId;
    private String processName;
    private String taskType;
    private int taskId;
    private ProcessContext context;

    public TaskRequest() {
    }

    public TaskRequest(String processId, String processName, String taskType, int taskId, ProcessContext context) {
        this.processId = processId;
        this.processName = processName;
        this.taskType = taskType;
        this.taskId = taskId;
        this.context = context;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public ProcessContext getContext() {
        return context;
    }

    public void setContext(ProcessContext context) {
        this.context = context;
    }

    @Override
    public Class<TaskRequestHandler> getJobRequestHandler() {
        return TaskRequestHandler.class;
    }
}
