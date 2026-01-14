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
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Jerome Blanchard
 */
@Entity
@NamedQueries({
        @NamedQuery(name = "Process.findByOwner", query = "SELECT p FROM Process p WHERE p.owner = :owner"),
        @NamedQuery(name = "Process.findByStore", query = "SELECT p FROM Process p WHERE p.store = :store"),
        @NamedQuery(name = "Process.findByStoreAndStatus", query = "SELECT p FROM Process p WHERE p.store = :store AND p.status IN :status")
})
@Table(indexes = {
        @Index(name = "process_idx", columnList = "owner"),
        @Index(name = "process_idx", columnList = "store"),
        @Index(name = "process_idx", columnList = "store, status")
})
public class Process {

    @Id
    private String id;
    private String owner;
    private String store;
    private String name;
    @Column(name = "tasks")
    private String tasks;
    @Transient
    private List<String> taskList;
    @Lob
    @Convert(converter = ProcessContextConverter.class)
    private ProcessContext context;
    @Enumerated(EnumType.STRING)
    private ProcessStatus status;
    private int nextTaskId;
    private String runningTaskJobId;
    private Long creationDate = -1L;
    private Long startDate = -1L;
    private Long endDate = -1L;

    public Process() {
    }

    public Process(ProcessDefinition process) {
        this.id = UUID.randomUUID().toString();
        this.store = process.getStore();
        this.name = process.getName();
        this.tasks = String.join(",", process.getTasks());
        this.taskList = process.getTasks();
        this.context = process.getContext();
        this.nextTaskId = 0;
        this.creationDate = System.currentTimeMillis();
        this.status = ProcessStatus.CREATED;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getStore() {
        return store;
    }

    public void setStore(String store) {
        this.store = store;
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

    public ProcessStatus getStatus() {
        return status;
    }

    public void setStatus(ProcessStatus status) {
        this.status = status;
    }

    public int getNextTaskId() {
        return nextTaskId;
    }

    public void setNextTaskId(int nextTaskIndex) {
        this.nextTaskId = nextTaskIndex;
    }

    public void incNextTaskIndex() {
        this.nextTaskId++;
    }

    public String getNextTask() {
        return (!this.isFinished() && this.getTasks() != null && this.getTaskList().size() >= nextTaskId) ?
                this.getTaskList().get(nextTaskId -1) : null;
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
}
