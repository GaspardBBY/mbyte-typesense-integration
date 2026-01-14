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
package fr.jayblanc.mbyte.manager.runtime.docker.task;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Network;
import fr.jayblanc.mbyte.manager.process.TaskException;
import fr.jayblanc.mbyte.manager.process.TaskStatus;
import fr.jayblanc.mbyte.manager.process.entity.ProcessContext;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Jerome Blanchard
 */
@QuarkusTest
public class CreateNetworkTaskTest {

    private static final Logger LOGGER = Logger.getLogger(CreateNetworkTaskTest.class.getName());
    private static final String NETWORK_NAME_PREFIX = "test_create_network_";

    @Inject CreateNetworkTaskHandler handler;
    @Inject DockerClient client;

    @AfterEach
    public void cleanupNetworks() {
        List<Network> networks = client.listNetworksCmd().withNameFilter(NETWORK_NAME_PREFIX).exec();
        for (Network network: networks) {
            LOGGER.log(Level.INFO, "Deleting network: " + network.getName());
            client.removeNetworkCmd(network.getId()).exec();
        }
    }

    @Test
    @Transactional
    public void testBadContext() {
        LOGGER.log(Level.INFO, "Testing bad context for network create...");
        assertThrows(TaskException.class, handler::execute, "Expected TaskException for bad context");
        assertEquals(TaskStatus.FAILED, handler.getStatus(), "Expected task status to be FAILED for bad context");
    }

    @Test
    @Transactional
    public void testCreateExistingNetwork() throws TaskException {
        LOGGER.log(Level.INFO, "Testing create existing 'bridge' network...");
        ProcessContext context = new ProcessContext();
        context.setValue(LookupNetworkTaskHandler.NETWORK_NAME, "bridge");
        handler.setContext(context);
        handler.execute();
        assertTrue(handler.getLog().contains("Found existing network with name: 'bridge'"), "Expected log to contain success message for already existing network");
        assertEquals(TaskStatus.COMPLETED, handler.getStatus(), "Expected task status to be COMPLETED for already existing network");
    }

    @Test
    @Transactional
    public void testCreateNetwork() throws TaskException {
        LOGGER.log(Level.INFO, "Testing creating new network...");
        ProcessContext context = new ProcessContext();
        String networkName = NETWORK_NAME_PREFIX + System.currentTimeMillis();
        context.setValue(LookupNetworkTaskHandler.NETWORK_NAME, networkName);
        handler.setContext(context);
        handler.execute();
        assertTrue(handler.getLog().contains("Created network with name: '" + networkName + "'"), "Expected log to contain success message for created network");
        assertEquals(TaskStatus.COMPLETED, handler.getStatus(), "Expected task status to be COMPLETED for existing network");
    }

}
