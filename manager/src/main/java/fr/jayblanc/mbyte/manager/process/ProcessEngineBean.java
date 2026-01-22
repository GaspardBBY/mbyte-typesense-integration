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

import fr.jayblanc.mbyte.manager.auth.AuthenticationService;
import fr.jayblanc.mbyte.manager.core.AccessDeniedException;
import fr.jayblanc.mbyte.manager.process.entity.Process;
import fr.jayblanc.mbyte.manager.process.entity.ProcessContext;
import fr.jayblanc.mbyte.manager.process.entity.ProcessStatus;
import fr.jayblanc.mbyte.manager.process.task.TaskRequest;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.logging.Logger;

/**
 * @author Jerome Blanchard
 */
@ApplicationScoped
public class ProcessEngineBean implements ProcessEngine, ProcessEngineAdmin {

    private static final Logger LOGGER = Logger.getLogger(ProcessEngineBean.class.getName());

    @Inject AuthenticationService auth;
    @Inject Event<TaskEvent> taskEvent;
    @Inject EntityManager em;

    @PostConstruct
    public void init() {
        LOGGER.info("ProcessEngineBean initialized");
        //TODO reload all process that are not finished and schedule their next task (or maybe rollback if a task was interrupted (no way to know at the moment))
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public String startProcess(ProcessDefinition processDef) throws ProcessAlreadyRunningException {
        LOGGER.info("Starting new process: " + processDef.getName());
        if (this.findRunningProcessesForApp(processDef.getAppId()).stream().anyMatch(p -> p.getName().equals(processDef.getName()))) {
            throw new ProcessAlreadyRunningException("A process with name: " + processDef.getName() + " is already running for application with id: " + processDef.getAppId());
        }
        Process process = new Process(processDef);
        process.setOwner(auth.getConnectedIdentifier());
        process.setNextTaskId(process.findFirstTask());
        process.setStartDate(System.currentTimeMillis());
        em.persist(process);
        this.scheduleNextTask(process);
        return process.getId();
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public Process getProcess(String id) throws ProcessNotFoundException, AccessDeniedException {
        LOGGER.info("Getting process: " + id);
        Process process = this.findById(id);
        if (!auth.getConnectedIdentifier().equals(process.getOwner())) {
            throw new AccessDeniedException("The process with id: " + id + " is not owned by the connected user");
        }
        return process;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public List<Process> findRunningProcessesForApp(String appId) {
        LOGGER.info("Finding running processes for application: " + appId);
        return em.createNamedQuery("Process.findByAppAndStatus", Process.class)
                .setParameter("appId", appId)
                .setParameter("status", ProcessStatus.getRunningProcessStatuses()).getResultList();
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public List<Process> findAllProcessesForApp(String appId) {
        LOGGER.info("Finding all processes for application: " + appId);
        return em.createNamedQuery("Process.findByApp", Process.class).setParameter("appId", appId).getResultList();
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void assignTask(String processId, String taskId, String jobId) throws ProcessNotFoundException {
        Process instance = this.findById(processId);
        LOGGER.info( "Process." + instance.getName() + "[" + instance.getId() + "]" + ".task[" + taskId + "] assigned");
        instance.setRunningTaskJobId(jobId);
        instance.setStatus(ProcessStatus.TASK_ASSIGNED);
        instance.appendLog("Process." + instance.getName()).appendLog(".task[").appendLog(taskId).appendLog("] assigned to job: ").appendLog(jobId).appendLog("\n");
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void startTask(String processId, String taskId) throws ProcessNotFoundException {
        Process instance = this.findById(processId);
        LOGGER.info( "Process." + instance.getName() + "[" + instance.getId() + "]" + ".task[" + taskId + "] running");
        instance.setStatus(ProcessStatus.TASK_RUNNING);
        instance.appendLog("Process." + instance.getName()).appendLog(".task[").appendLog(taskId).appendLog("] starting").appendLog("\n");
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void completeTask(String processId, String taskId, String taskLog, ProcessContext ctx) throws ProcessNotFoundException {
        Process instance = this.findById(processId);
        LOGGER.info( "Process." + instance.getName() + "[" + instance.getId() + "]" + ".task[" + taskId + "] completed");
        instance.appendLog(taskLog);
        instance.setContext(ctx);
        instance.setStatus(ProcessStatus.PENDING);
        instance.appendLog("Process." + instance.getName()).appendLog(".task[").appendLog(taskId).appendLog("] completed").appendLog("\n");
        instance.setNextTaskId(instance.findNextTask(taskId));
        this.scheduleNextTask(instance);
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void failTask(String processId, String taskId, String taskLog, TaskException wte, ProcessContext ctx) {
        Process instance = em.find(Process.class, processId);
        if (instance == null) {
            LOGGER.severe("Unable to find process instance for id: " + processId + " while failing task");
            return;
        }
        LOGGER.info( "Process." + instance.getName() + "[" + instance.getId() + "]" + ".task[" + taskId + "] failed");
        instance.appendLog(taskLog);
        if (ctx != null) {
            instance.setContext(ctx);
        }
        instance.setStatus(ProcessStatus.FAILED);
        instance.appendLog("Process." + instance.getName()).appendLog(".task[").appendLog(taskId).appendLog("] failed: ").appendLog(wte.getMessage()).appendLog("\n");
        instance.setEndDate(System.currentTimeMillis());
        //TODO If the process is configured to rollback on failure, schedule rollback tasks
    }

    private void scheduleNextTask(Process instance) {
        if (instance.hasNextTask()) {
            LOGGER.info( "Process." + instance.getName() + "[" + instance.getId() + "]" + ".task[" + instance.getNextTaskId() + "]: scheduled");
            TaskRequest taskRequest = new TaskRequest(instance.getId(), instance.getName(), instance.getNextTaskId(), instance.getContext());
            taskEvent.fire(new TaskEvent(taskRequest));
            LOGGER.info("Event fired for " + instance.getName() + "[" + instance.getId() + "]" + ".task[" + instance.getNextTaskId() + "], job will be enqueued after transaction commit");
            instance.setStatus(ProcessStatus.PENDING);
        } else {
            LOGGER.info( "Process." + instance.getName() + "[" + instance.getId() + "] completed");
            instance.setEndDate(System.currentTimeMillis());
            instance.setStatus(ProcessStatus.COMPLETED);
        }
    }

    private Process findById(String id) throws ProcessNotFoundException {
        Process instance = em.find(Process.class, id);
        if ( instance == null ) {
            throw new ProcessNotFoundException("Unable to find a process instance for id: " + id);
        }
        return instance;
    }

}
