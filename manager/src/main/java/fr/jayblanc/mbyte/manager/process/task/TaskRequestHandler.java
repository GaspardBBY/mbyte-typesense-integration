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

import fr.jayblanc.mbyte.manager.process.ProcessEngineAdmin;
import fr.jayblanc.mbyte.manager.process.TaskException;
import fr.jayblanc.mbyte.manager.process.Task;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.UserTransaction;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Jerome Blanchard
 */
@ApplicationScoped
public class TaskRequestHandler implements JobRequestHandler<TaskRequest> {

    private static final Logger LOGGER = Logger.getLogger(TaskRequestHandler.class.getName());

    @Inject UserTransaction tx;
    @Inject Instance<Task> handlers;
    @Inject ProcessEngineAdmin engine;

    @PostConstruct
    public void init() {
        LOGGER.info("TaskRequestHandler initialized with " + handlers.stream().count() + " workflow tasks");
    }

    @Override
    public void run(TaskRequest taskRequest) {
        try {
            LOGGER.log(Level.INFO, "Starting process task request handler");
            tx.begin();
            Optional<Task> taskHandler = handlers.stream().filter(t -> t.getTaskName().equals(taskRequest.getTaskType())).findFirst();
            try {
                if (taskHandler.isPresent()) {
                    Task handler = taskHandler.get();
                    handler.setTaskId(taskRequest.getTaskId());
                    handler.setContext(taskRequest.getContext());
                    LOGGER.info( "Process." + taskRequest.getProcessName() + "[" + taskRequest.getProcessId() + "]" + ".task[" + taskRequest.getTaskId() + "]: starting");
                    engine.startTask(taskRequest.getProcessId(), taskRequest.getTaskId());
                    handler.execute();
                    LOGGER.info( "Process." + taskRequest.getProcessName() + "[" + taskRequest.getProcessId() + "]" + ".task[" + taskRequest.getTaskId() + "]: completed successfully");
                    engine.completeTask(taskRequest.getProcessId(), taskRequest.getTaskId(), handler.getLog(), handler.getContext());
                } else {
                    LOGGER.log(Level.SEVERE, "No process task handler found for task type: " + taskRequest.getTaskType());
                    throw new TaskException("No process task handler found for task type: " + taskRequest.getTaskType());
                }
            } catch (TaskException wte) {
                LOGGER.info( "Process." + taskRequest.getProcessName() + "[" + taskRequest.getProcessId() + "]" + ".task[" + taskRequest.getTaskId() + "]: failed with exception: " + wte.getMessage());
                engine.failTask(taskRequest.getProcessId(), taskRequest.getTaskId(), taskHandler.isPresent()?taskHandler.get().getLog():"", wte, taskHandler.isPresent()?taskHandler.get().getContext():null);
            }
            tx.commit();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Process task request handler transaction problem", e);
            try {
                tx.rollback();
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Process task request handler transaction rollback problem", ex);
            }
        }
    }
}
