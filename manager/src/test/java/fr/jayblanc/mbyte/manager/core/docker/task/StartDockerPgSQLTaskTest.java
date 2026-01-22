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
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Network;
import fr.jayblanc.mbyte.manager.core.runtime.task.database.StartDockerPgSQLTask;
import fr.jayblanc.mbyte.manager.process.TaskException;
import fr.jayblanc.mbyte.manager.process.TaskStatus;
import fr.jayblanc.mbyte.manager.process.entity.ProcessContext;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Jerome Blanchard
 */
@QuarkusTest
public class StartDockerPgSQLTaskTest {

    private static final Logger LOGGER = Logger.getLogger(StartDockerPgSQLTaskTest.class.getName());

    private static final String NETWORK_NAME_PREFIX = "test_start_docker_pgsql_network_";
    private static final String VOLUME_NAME_PREFIX = "test_start_docker_pgsql_volume_";
    private static final String CONTAINER_NAME_PREFIX = "test_start_docker_pgsql_container_";

    @Inject StartDockerPgSQLTask handler;
    @Inject DockerClient client;

    @AfterEach
    public void cleanup() {
        // Containers first (they may use volumes/networks)
        List<Container> containers = client.listContainersCmd().withShowAll(true).exec();
        for (Container container : containers) {
            if (container.getNames() == null) continue;
            for (String name : container.getNames()) {
                // docker-java returns names with a leading '/'
                if (name != null && name.startsWith("/" + CONTAINER_NAME_PREFIX)) {
                    LOGGER.log(Level.INFO, "Deleting container: " + name + " (id=" + container.getId() + ")");
                    try {
                        // Ensure stopped then removed
                        try {
                            client.stopContainerCmd(container.getId()).exec();
                        } catch (RuntimeException ignored) {
                            // already stopped
                        }
                        client.removeContainerCmd(container.getId()).withForce(true).withRemoveVolumes(true).exec();
                    } catch (RuntimeException e) {
                        LOGGER.log(Level.WARNING, "Failed to delete container: " + name + " - " + e.getMessage());
                    }
                }
            }
        }

        // Networks
        List<Network> networks = client.listNetworksCmd().withNameFilter(NETWORK_NAME_PREFIX).exec();
        for (Network network : networks) {
            LOGGER.log(Level.INFO, "Deleting network: " + network.getName());
            try {
                client.removeNetworkCmd(network.getId()).exec();
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "Failed to delete network: " + network.getName() + " - " + e.getMessage());
            }
        }

        // Volumes (best-effort)
        var volumesResponse = client.listVolumesCmd().exec();
        if (volumesResponse != null && volumesResponse.getVolumes() != null) {
            for (var volume : volumesResponse.getVolumes()) {
                if (volume.getName() != null && volume.getName().startsWith(VOLUME_NAME_PREFIX)) {
                    LOGGER.log(Level.INFO, "Deleting volume: " + volume.getName());
                    try {
                        client.removeVolumeCmd(volume.getName()).exec();
                    } catch (RuntimeException e) {
                        LOGGER.log(Level.WARNING, "Failed to delete volume: " + volume.getName() + " - " + e.getMessage());
                    }
                }
            }
        }
    }

    @Test
    @Transactional
    public void testBadContext() {
        LOGGER.log(Level.INFO, "Testing bad context for start database...");
        assertThrows(TaskException.class, handler::execute, "Expected TaskException for bad context");
        assertEquals(TaskStatus.FAILED, handler.getStatus(), "Expected task status to be FAILED for bad context");
    }

    @Test
    @Transactional
    public void testStartDatabase() throws TaskException {
        LOGGER.log(Level.INFO, "Testing starting pgsql container...");

        String suffix = Long.toString(System.currentTimeMillis());
        String networkName = NETWORK_NAME_PREFIX + suffix;
        String volumeName = VOLUME_NAME_PREFIX + suffix;
        String containerName = CONTAINER_NAME_PREFIX + suffix;

        // Pre-create network and volume required by the handler
        client.createNetworkCmd().withName(networkName).exec();
        client.createVolumeCmd().withName(volumeName).exec();

        ProcessContext context = new ProcessContext();
        context.setValue(StartDockerPgSQLTask.NETWORK_NAME, networkName);
        context.setValue(StartDockerPgSQLTask.DB_VOLUME_NAME, volumeName);
        context.setValue(StartDockerPgSQLTask.DB_CONTAINER_NAME, containerName);
        context.setValue(StartDockerPgSQLTask.DB_USER, "test");
        context.setValue(StartDockerPgSQLTask.DB_PASSWORD, "test");
        context.setValue(StartDockerPgSQLTask.DB_NAME, "testdb");
        handler.setContext(context);

        handler.execute();

        assertEquals(TaskStatus.COMPLETED, handler.getStatus(), "Expected task status to be COMPLETED");
        assertTrue(handler.getLog().contains("Database container"), "Expected log to mention database container");

        Optional<Container> created = client.listContainersCmd().withShowAll(true).exec().stream()
                .filter(c -> c.getNames() != null)
                .filter(c -> java.util.Arrays.stream(c.getNames()).anyMatch(n -> ("/" + containerName).equals(n)))
                .findFirst();

        //TODO Test a connection to the db

        assertTrue(created.isPresent(), "Expected container to exist after handler execution");
    }
}

