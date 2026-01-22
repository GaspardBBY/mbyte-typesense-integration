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

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateNetworkResponse;
import com.github.dockerjava.api.model.Network;
import fr.jayblanc.mbyte.manager.core.runtime.task.network.LookupDockerNetworkTask;
import fr.jayblanc.mbyte.manager.core.runtime.task.network.RemoveDockerNetworkTask;
import fr.jayblanc.mbyte.manager.process.TaskException;
import fr.jayblanc.mbyte.manager.process.TaskStatus;
import fr.jayblanc.mbyte.manager.process.entity.ProcessContext;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Jerome Blanchard
 */
@QuarkusTest
public class RemoveDockerNetworkTaskTest {

    private static final Logger LOGGER = Logger.getLogger(RemoveDockerNetworkTaskTest.class.getName());
    private static final String NETWORK_NAME_PREFIX = "test_remove_docker_network_";

    @Inject RemoveDockerNetworkTask handler;
    @Inject DockerClient client;

    private String networkName;

    @BeforeEach
    public void setupNetworks() {
        networkName = NETWORK_NAME_PREFIX + System.currentTimeMillis();
        CreateNetworkResponse response = client.createNetworkCmd().withName(networkName).exec();
        if (response.getId() == null) {
            throw new RuntimeException("Failed to create network for test setup with name: " + networkName);
        }
    }

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
        LOGGER.log(Level.INFO, "Testing bad context for network delete...");
        assertThrows(TaskException.class, handler::execute, "Expected TaskException for bad context");
        assertEquals(TaskStatus.FAILED, handler.getStatus(), "Expected task status to be FAILED for bad context");
    }

    @Test
    @Transactional
    public void testRemoveUnexistingNetwork() throws TaskException {
        LOGGER.log(Level.INFO, "Testing remove 'unexisting' network...");
        ProcessContext context = new ProcessContext();
        context.setValue(LookupDockerNetworkTask.NETWORK_NAME, "unexisting");
        handler.setContext(context);
        handler.execute();
        assertTrue(handler.getLog().contains("No network with name: 'unexisting', nothing done"), "Expected log to contain success message for unexisting network");
        assertEquals(TaskStatus.COMPLETED, handler.getStatus(), "Expected task status to be COMPLETED for unexisting network");
    }

    @Test
    @Transactional
    public void testRemoveNetwork() throws TaskException {
        LOGGER.log(Level.INFO, "Testing remove network...");
        ProcessContext context = new ProcessContext();
        context.setValue(LookupDockerNetworkTask.NETWORK_NAME, networkName);
        handler.setContext(context);
        handler.execute();
        assertTrue(handler.getLog().contains("Removed network with name: '" + networkName + "'"), "Expected log to contain success message for deleted network");
        assertEquals(TaskStatus.COMPLETED, handler.getStatus(), "Expected task status to be COMPLETED for deleted network");
    }

}
