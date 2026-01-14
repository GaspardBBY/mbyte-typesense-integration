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

import fr.jayblanc.mbyte.manager.exception.AccessDeniedException;
import fr.jayblanc.mbyte.manager.process.entity.Process;
import fr.jayblanc.mbyte.manager.process.entity.ProcessStatus;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Jerome Blanchard
 */
@QuarkusTest
public class ProcessEngineTest {

    private static Logger LOGGER = Logger.getLogger(ProcessEngineTest.class.getName());

    @Inject ProcessEngine engine;

    @Test
    @Transactional
    void testProcessWithDummyTask()
            throws InterruptedException, AccessDeniedException, ProcessNotFoundException, ProcessAlreadyRunningException {
        ProcessDefinition definition = ProcessDefinition.builder()
                .withName("TestDummyProcess")
                .addTask(DummyTestTaskHandler.TASK_NAME)
                .setTaskContextEntry(DummyTestTaskHandler.HELLO_NAME, "Sheldon")
                .build();
        String pid = engine.startProcess(definition);

        long start = System.currentTimeMillis();
        Process process = engine.getProcess(pid);
        while (!process.isFinished()) {
            Thread.sleep(1000);
            if ((System.currentTimeMillis() - start) > 30000) {
                fail("Timed out waiting for process to complete");
            };
            process = engine.getProcess(pid);
        }

        assertEquals("TestDummyProcess", process.getName(), "The name should not have changed");
        assertNotNull(process.getCreationDate());
        assertNotNull(process.getStartDate());
        assertNotNull(process.getEndDate());
        assertFalse(process.isRunning());
        assertTrue(process.isFinished());
        assertEquals(ProcessStatus.COMPLETED, process.getStatus());
        assertTrue(process.getCreationDate() <= process.getStartDate(), "The start date should be after the creation date");
        assertTrue(process.getStartDate() < process.getEndDate(), "The end date should be after the start date");
        assertFalse(process.getContext().getLog().isEmpty());
        assertEquals("Hello Sheldon !", process.getContext().getStringValue(DummyTestTaskHandler.HELLO));
        LOGGER.log(Level.INFO, process.getContext().getLog());
        assertTrue(process.getContext().getLog().contains(DummyTestTaskHandler.TASK_NAME.concat(" executed successfully")), "The log should contain the task execution log");
    }

    @Test
    @Transactional
    void testProcessWithTwoDummyTask()
            throws InterruptedException, AccessDeniedException, ProcessNotFoundException, ProcessAlreadyRunningException {
        ProcessDefinition definition = ProcessDefinition.builder()
                .withName("TestDoubleDummyProcess")
                .addTask(DummyTestTaskHandler.TASK_NAME)
                .setTaskContextEntry(DummyTestTaskHandler.HELLO_NAME, "Sheldon")
                .addTask(DummyTestTaskHandler.TASK_NAME)
                .setTaskContextEntry(DummyTestTaskHandler.HELLO_NAME, "Rajesh")
                .build();
        String pid = engine.startProcess(definition);

        long start = System.currentTimeMillis();
        Process process = engine.getProcess(pid);
        while (!process.isFinished()) {
            Thread.sleep(1000);
            if ((System.currentTimeMillis() - start) > 30000) {
                fail("Timed out waiting for process to complete");
            };
            process = engine.getProcess(pid);
        }

        assertEquals("TestDoubleDummyProcess", process.getName(), "The name should not have changed");
        assertEquals("Hello Rajesh !", process.getContext().getStringValue(DummyTestTaskHandler.HELLO));
    }

    @Test
    @Transactional
    void testProcessWithFailingTask()
            throws InterruptedException, AccessDeniedException, ProcessNotFoundException, ProcessAlreadyRunningException {
        ProcessDefinition definition = ProcessDefinition.builder()
                .withName("TestFailingProcess")
                .addTask(DummyTestTaskHandler.TASK_NAME)
                .setTaskContextEntry(DummyTestTaskHandler.HELLO_NAME, "Rajesh")
                .addTask(FailingTestTaskHandler.NAME)
                .build();
        String pid = engine.startProcess(definition);

        long start = System.currentTimeMillis();
        Process process = engine.getProcess(pid);
        while (!process.isFinished()) {
            Thread.sleep(1000);
            if ((System.currentTimeMillis() - start) > 30000) {
                fail("Timed out waiting for process to complete");
            };
            process = engine.getProcess(pid);
        }

        assertEquals("TestFailingProcess", process.getName(), "The name should not have changed");
        assertFalse(process.isRunning());
        assertTrue(process.isFinished());
        assertEquals(ProcessStatus.FAILED, process.getStatus());
        assertTrue(process.getCreationDate() <= process.getStartDate(), "The start date should be after the creation date");
        assertTrue(process.getStartDate() < process.getEndDate(), "The end date should be after the start date");
        assertFalse(process.getContext().getLog().isEmpty());
    }
}
