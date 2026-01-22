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
package fr.jayblanc.mbyte.manager.process.entity;

import fr.jayblanc.mbyte.manager.process.ProcessDefinition;
import fr.jayblanc.mbyte.manager.process.Task;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Jerome Blanchard
 */
@Entity
@NamedQueries({
        @NamedQuery(name = "Process.findByOwner", query = "SELECT p FROM Process p WHERE p.owner = :owner"),
        @NamedQuery(name = "Process.findByApp", query = "SELECT p FROM Process p WHERE p.appId = :appId"),
        @NamedQuery(name = "Process.findByAppAndStatus", query = "SELECT p FROM Process p WHERE p.appId = :appId AND p.status IN :status")
})
@Table( name="proc",
        indexes = {
        @Index(name = "process_idx", columnList = "owner"),
        @Index(name = "process_idx", columnList = "appId"),
        @Index(name = "process_idx", columnList = "appId, status")
})
public class Process {

    @Id
    private String id;
    private String owner;
    private String appId;
    private String name;
    private String tasks;
    @Transient
    private List<String> taskList;
    @Lob
    @Convert(converter = ProcessContextConverter.class)
    private ProcessContext context;
    @Lob
    private String log;
    @Enumerated(EnumType.STRING)
    private ProcessStatus status;
    private String nextTaskId;
    private String runningTaskJobId;
    private Long creationDate = -1L;
    private Long startDate = -1L;
    private Long endDate = -1L;

    public Process() {
    }

    public Process(ProcessDefinition definition) {
        this.id = UUID.randomUUID().toString();
        this.appId = definition.getAppId();
        this.name = definition.getName();
        this.tasks = String.join(",", definition.getTasks());
        this.taskList = definition.getTasks();
        this.context = definition.getContext();
        this.creationDate = System.currentTimeMillis();
        this.status = ProcessStatus.CREATED;
        this.log = "";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public synchronized  List<String> getTaskList() {
        if (taskList == null) {
            taskList = (tasks == null || tasks.isEmpty()) ? new ArrayList<>() :
                    Arrays.stream(tasks.split(",")).collect(Collectors.toList());
        }
        return taskList;
    }

    public String getTasks() {
        return tasks;
    }

    public void setTasks(String tasks) {
        this.tasks = tasks;
    }

    public ProcessContext getContext() {
        return context;
    }

    public  void setContext(ProcessContext context) {
        this.context = context;
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }

    public Process appendLog(String value) {
        this.log = this.log.concat(value);
        return this;
    }

    public ProcessStatus getStatus() {
        return status;
    }

    public void setStatus(ProcessStatus status) {
        this.status = status;
    }

    public String getNextTaskId() {
        return nextTaskId;
    }

    public void setNextTaskId(String nextTaskId) {
        this.nextTaskId = nextTaskId;
    }

    public boolean hasNextTask() {
        if (this.isFinished() || this.nextTaskId == null) {
            return false;
        }
        return this.getTaskList().contains(this.nextTaskId);
    }

    public String findFirstTask() {
        List<String> ordered = getOrderedTasks();
        return ordered.isEmpty() ? null : ordered.get(0);
    }

    public String findNextTask(String taskId) {
        List<String> ordered = getOrderedTasks();
        if (ordered.isEmpty() || taskId == null) {
            return null;
        }
        int idx = ordered.indexOf(taskId);
        if (idx < 0) {
            return null;
        }
        int nextIdx = idx + 1;
        if (nextIdx >= ordered.size()) {
            return null;
        }
        return ordered.get(nextIdx);
    }

    public String getRunningTaskJobId() {
        return runningTaskJobId;
    }

    public void setRunningTaskJobId(String runningTaskId) {
        this.runningTaskJobId = runningTaskId;
    }

    public Long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Long creationDate) {
        this.creationDate = creationDate;
    }

    public Long getStartDate() {
        return startDate;
    }

    public void setStartDate(Long startDate) {
        this.startDate = startDate;
    }

    public Long getEndDate() {
        return endDate;
    }

    public void setEndDate(Long endDate) {
        this.endDate = endDate;
    }

    public boolean isRunning() {
        return status.isRunning();
    }

    public boolean isFinished() {
        return status.isFinal();
    }


    private synchronized List<String> getOrderedTasks() {
        List<String> list = getTaskList();
        list.sort(TASK_ID_COMPARATOR);
        return list;
    }

    private static final Comparator<String> TASK_ID_COMPARATOR = Comparator
            .comparingInt((String t) -> parseTaskNumber(t).orElse(Integer.MAX_VALUE))
            .thenComparing(Process::parseTaskType, Comparator.nullsLast(String::compareTo))
            .thenComparing(Comparator.nullsLast(String::compareTo));

    private static java.util.OptionalInt parseTaskNumber(String taskId) {
        if (taskId == null) {
            return java.util.OptionalInt.empty();
        }
        int dot = taskId.indexOf('.');
        if (dot <= 0) {
            return java.util.OptionalInt.empty();
        }
        String prefix = taskId.substring(0, dot);
        try {
            return java.util.OptionalInt.of(Integer.parseInt(prefix));
        } catch (NumberFormatException e) {
            return java.util.OptionalInt.empty();
        }
    }

    private static String parseTaskType(String taskId) {
        if (taskId == null) {
            return null;
        }
        int dot = taskId.indexOf('.');
        if (dot < 0 || dot == taskId.length() - 1) {
            return null;
        }
        return taskId.substring(dot + 1);
    }
}
