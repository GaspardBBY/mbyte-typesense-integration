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
import java.util.Optional;

/**
 * @author Jerome Blanchard
 */
public abstract class Task {

    private ProcessContext context;
    private TaskStatus status;
    private StringBuilder log;
    private String taskId;
    private int progress;

    public Task() {
        this.context = new ProcessContext();
        this.status = TaskStatus.CREATED;
        this.log = new StringBuilder();
        this.progress = 0;
    }

    abstract public String getTaskName();

    abstract public void execute() throws TaskException;

    public void rollback() throws TaskException {
        this.rollback("Default rollback, nothing done.");
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void updateStatus(TaskStatus status) {
        TaskStatus before = this.status;
        this.status = status;
        this.log("status changed from: '" + before.name() + "' to: '" + status.name() + "'");
    }

    public void setContext(ProcessContext context) {
        this.context = context;
    }

    public ProcessContext getContext() {
        return this.context;
    }

    public Optional<String> getContextValue(String key) {
        return this.context.getStringValue(taskId, key);
    }

    public String getContextValue(String key, String defaultValue) {
        return this.getContextValue(key).orElse(defaultValue);
    }

    public <T> Optional<T> getContextValue(String key, Class<T> clazz) {
        return this.context.getValue(taskId, key, clazz);
    }

    public <T> T getContextValue(String key, Class<T> clazz, T defaultValue) {
        return this.getContextValue(key, clazz).orElse(defaultValue);
    }

    public String getMandatoryContextValue(String key) throws TaskException {
        Optional<String> value = this.context.getStringValue(taskId, key);
        if (value.isEmpty()) {
            this.fail(key.concat(" is not set in the context"));
            throw new TaskException("Missing mandatory context parameter: " + key);
        }
        return value.get();
    }

    public void setContextValue(String key, Serializable value) {
        this.context.setValue(taskId, key, value);
    }

    public void unsetContextValue(String key) {
        this.context.unsetValue(taskId, key);
    }

    public void setGlobalContextValue(String key, Serializable value) {
        this.context.setValue(key, value);
    }

    public void unsetGlobalContextValue(String key) {
        this.context.unsetValue(key);
    }

    public String getLog() {
        return this.log.toString();
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        int before = this.progress;
        this.progress = progress;
        this.log("progress changed from: '" + before + "%' to: '" + progress + "%'");
    }

    public void log(String message) {
        this.appendLog("[").appendLog(getTaskName()).appendLog("] ").appendLog(message).appendLog("\n");
    }

    public void fail(String reason) {
        this.updateStatus(TaskStatus.FAILED);
        this.log("Task failed due to: " + reason);
    }

    public void complete(String reason) {
        this.updateStatus(TaskStatus.COMPLETED);
        this.setProgress(100);
        this.log("Task completed: " + reason);
    }

    public void rollback(String reason) {
        this.updateStatus(TaskStatus.ROLLED_BACK);
        this.setProgress(0);
        this.log("Task rollback completed: " + reason);
    }

    public Task appendLog(String value) {
        this.log.append(value);
        return this;
    }

    public Task appendLog(Object value) {
        this.log.append(value);
        return this;
    }


}
