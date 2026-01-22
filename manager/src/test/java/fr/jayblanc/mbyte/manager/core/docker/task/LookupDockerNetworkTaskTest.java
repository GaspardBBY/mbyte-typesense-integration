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
package fr.jayblanc.mbyte.manager.core.docker.task;

import fr.jayblanc.mbyte.manager.core.runtime.task.network.LookupDockerNetworkTask;
import fr.jayblanc.mbyte.manager.process.TaskException;
import fr.jayblanc.mbyte.manager.process.TaskStatus;
import fr.jayblanc.mbyte.manager.process.entity.ProcessContext;
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
public class LookupDockerNetworkTaskTest {

    private static final Logger LOGGER = Logger.getLogger(LookupDockerNetworkTaskTest.class.getName());

    @Inject LookupDockerNetworkTask handler;

    @Test
    @Transactional
    public void testBadContext() {
        LOGGER.log(Level.INFO, "Testing bad context for network lookup...");
        assertThrows(TaskException.class, handler::execute, "Expected TaskException for bad context");
        assertEquals(TaskStatus.FAILED, handler.getStatus(), "Expected task status to be FAILED for bad context");
    }

    @Test
    @Transactional
    public void testUnexistingNetwork() {
        LOGGER.log(Level.INFO, "Testing unexisting network lookup...");
        ProcessContext context = new ProcessContext();
        context.setValue(LookupDockerNetworkTask.NETWORK_NAME, "test_unexisting");
        handler.setContext(context);
        assertThrows(TaskException.class, handler::execute, "Expected TaskException for unexisting network");
        assertTrue(handler.getLog().contains("Failed to find network with name: 'test_unexisting'"), "Expected log to contain failure message for unexisting network");
        assertEquals(TaskStatus.FAILED, handler.getStatus(), "Expected task status to be FAILED for unexisting network");
    }

    @Test
    @Transactional
    public void testBridgeNetwork() throws TaskException {
        LOGGER.log(Level.INFO, "Testing existing 'bridge' network lookup...");
        ProcessContext context = new ProcessContext();
        context.setValue(LookupDockerNetworkTask.NETWORK_NAME, "bridge");
        handler.setContext(context);
        handler.execute();
        assertTrue(handler.getLog().contains("Found existing network with name: 'bridge'"), "Expected log to contain success message for existing network");
        assertEquals(TaskStatus.COMPLETED, handler.getStatus(), "Expected task status to be COMPLETED for existing network");
    }

}
