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
import fr.jayblanc.mbyte.manager.process.TaskEvent;
import fr.jayblanc.mbyte.manager.process.TaskException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import org.jobrunr.jobs.JobId;
import org.jobrunr.scheduling.JobRequestScheduler;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Jerome Blanchard
 */
@ApplicationScoped
public class TaskEventListener {

    private static final Logger LOGGER = Logger.getLogger(TaskEventListener.class.getName());

    @Inject JobRequestScheduler jobScheduler;
    @Inject ProcessEngineAdmin engine;

    public void onTaskScheduled(@Observes(during = TransactionPhase.AFTER_SUCCESS) TaskEvent event) {
        LOGGER.log(Level.INFO, "Received event on transaction commit, enqueueing job for task: " + event.getTaskRequest().getTaskId());
        try {
            JobId jobId = jobScheduler.enqueue(event.getTaskRequest());
            engine.assignTask(event.getTaskRequest().getProcessId(), event.getTaskRequest().getTaskId(), jobId.toString());
            LOGGER.log(Level.INFO, "Job enqueued with ID: " + jobId + " for task: " + event.getTaskRequest().getTaskType());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to enqueue job for task: " + event.getTaskRequest().getTaskType(), e);
            engine.failTask(event.getTaskRequest().getProcessId(), event.getTaskRequest().getTaskId(), "", new TaskException("Failed to enqueue job", e), event.getTaskRequest().getContext());
        }
    }
}

