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
import fr.jayblanc.mbyte.manager.exception.AccessDeniedException;
import fr.jayblanc.mbyte.manager.process.entity.ProcessContext;
import fr.jayblanc.mbyte.manager.process.entity.Process;
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
    public String startProcess(ProcessDefinition process) throws ProcessAlreadyRunningException {
        LOGGER.info("Starting new process: " + process.getName());
        if (!process.isParallelRunAllowed() && this.findRunningProcessesForStore(process.getStore()).stream().anyMatch(p -> p.getName().equals(process.getName()))) {
            throw new ProcessAlreadyRunningException("A process with name: " + process.getName() + " is already running for store: " + process.getStore());
        }
        Process instance = new Process(process);
        instance.setOwner(auth.getConnectedIdentifier());
        instance.setNextTaskId(0);
        instance.setStartDate(System.currentTimeMillis());
        em.persist(instance);
        instance.incNextTaskIndex();
        this.scheduleNextTask(instance);
        return instance.getId();
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public Process getProcess(String id) throws ProcessNotFoundException, AccessDeniedException {
        LOGGER.info("Getting process: " + id);
        Process instance = this.findById(id);
        if (!auth.getConnectedIdentifier().equals(instance.getOwner())) {
            throw new AccessDeniedException("The process with id: " + id + " is not owned by the connected user");
        }
        return instance;
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<Process> findRunningProcessesForStore(String storeId) {
        LOGGER.info("Finding running processes for store: " + storeId);
        return em.createNamedQuery("Process.findByStoreAndStatus", Process.class)
                .setParameter("store", storeId)
                .setParameter("status", ProcessStatus.getRunningProcessStatuses()).getResultList();
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<Process> findAllProcessesForStore(String storeId) {
        LOGGER.info("Finding all processes for store: " + storeId);
        return em.createNamedQuery("Process.findByStore", Process.class).setParameter("store", storeId).getResultList();
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void assignTask(String processId, int taskId, String jobId) throws ProcessNotFoundException {
        Process instance = this.findById(processId);
        LOGGER.info( "Process." + instance.getName() + "[" + instance.getId() + "]" + ".task[" + instance.getNextTaskId() + "] assigned");
        instance.setRunningTaskJobId(jobId);
        instance.setStatus(ProcessStatus.TASK_ASSIGNED);
        instance.getContext().appendLog(instance.getName()).appendLog(".task[").appendLog(taskId).appendLog("] assigned to job: ").appendLog(jobId).appendLog("\n");
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void startTask(String processId, int taskId) throws ProcessNotFoundException {
        Process instance = this.findById(processId);
        LOGGER.info( "Process." + instance.getName() + "[" + instance.getId() + "]" + ".task[" + instance.getNextTaskId() + "] running");
        instance.setStatus(ProcessStatus.TASK_RUNNING);
        instance.getContext().appendLog(instance.getName()).appendLog(".task[").appendLog(taskId).appendLog("] starting").appendLog("\n");
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void completeTask(String processId, int taskId, ProcessContext ctx) throws ProcessNotFoundException {
        Process instance = this.findById(processId);
        LOGGER.info( "Process." + instance.getName() + "[" + instance.getId() + "]" + ".task[" + instance.getNextTaskId() + "] completed");
        //TODO merge context instead of replacing it or create a specific method to log something from tasks
        instance.setContext(ctx);
        instance.setStatus(ProcessStatus.PENDING);
        instance.getContext().appendLog(instance.getName()).appendLog(".task[").appendLog(taskId).appendLog("] completed").appendLog("\n");
        instance.incNextTaskIndex();
        this.scheduleNextTask(instance);
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void failTask(String processId, int taskId, ProcessContext ctx, TaskException wte) {
        Process instance = em.find(Process.class, processId);
        if (instance == null) {
            LOGGER.severe("Unable to find process instance for id: " + processId + " while failing task");
            return;
        }
        LOGGER.info( "Process." + instance.getName() + "[" + instance.getId() + "]" + ".task[" + instance.getNextTaskId() + "] failed");
        instance.setContext(ctx);
        instance.setStatus(ProcessStatus.FAILED);
        instance.getContext().appendLog(instance.getName()).appendLog(".task[").appendLog(taskId).appendLog("] failed: ").appendLog(wte.getMessage()).appendLog("\n");
        instance.setEndDate(System.currentTimeMillis());
        //TODO If the process is configured to rollback on failure, schedule rollback tasks
    }

    private void scheduleNextTask(Process instance) {
        String nextTask = instance.getNextTask();
        if (nextTask != null) {
            LOGGER.info( "Process." + instance.getName() + "[" + instance.getId() + "]" + ".task[" + instance.getNextTaskId() + "]: scheduled");
            TaskRequest taskRequest = new TaskRequest(instance.getId(), instance.getName(), nextTask, instance.getNextTaskId(), instance.getContext());
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
