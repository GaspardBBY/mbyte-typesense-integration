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
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.api.model.Volume;
import fr.jayblanc.mbyte.manager.core.runtime.task.store.StartDockerStoreTask;
import fr.jayblanc.mbyte.manager.process.TaskException;
import fr.jayblanc.mbyte.manager.process.TaskStatus;
import fr.jayblanc.mbyte.manager.process.entity.ProcessContext;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for StartStoreTaskHandler.
 *
 * Note: this test assumes Docker is available locally and the store image exists.
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class StartDockerStoreTaskTest {

    private static final Logger LOGGER = Logger.getLogger(StartDockerStoreTaskTest.class.getName());

    private static final String NETWORK_NAME_PREFIX = "test_start_docker_store_network_";
    private static final String DB_VOLUME_NAME_PREFIX = "test_start_docker_store_db_volume_";
    private static final String DB_CONTAINER_NAME_PREFIX = "test_start_docker_store_db_container_";
    private static final String STORE_VOLUME_NAME_PREFIX = "test_start_docker_store_store_volume_";
    private static final String STORE_CONTAINER_NAME_PREFIX = "test_start_docker_store_store_container_";
    private static final String STORE_IMAGE = "jerome/store:25.1-SNAPSHOT";

    private static String networkName;
    private static String dbVolumeName;
    private static String dbContainerName;
    private static String storeVolumeName;
    private static String storeContainerName;

    @Inject StartDockerStoreTask storeHandler;
    @Inject DockerClient client;

    @BeforeAll
    public void setup() {
        // Fail fast if the store image is not available locally.
        boolean imageExists = client.listImagesCmd().withImageNameFilter(STORE_IMAGE).exec().stream()
                .flatMap(img -> img.getRepoTags() == null ? java.util.stream.Stream.empty() : Arrays.stream(img.getRepoTags()))
                .anyMatch(STORE_IMAGE::equals);
        assertTrue(imageExists, "Missing local Docker image: " + STORE_IMAGE + " (build the store module first)");

        String suffix = Long.toString(System.currentTimeMillis());

        networkName = NETWORK_NAME_PREFIX + suffix;
        dbVolumeName = DB_VOLUME_NAME_PREFIX + suffix;
        dbContainerName = DB_CONTAINER_NAME_PREFIX + suffix;
        storeVolumeName = STORE_VOLUME_NAME_PREFIX + suffix;
        storeContainerName = STORE_CONTAINER_NAME_PREFIX + suffix;

        LOGGER.log(Level.INFO, "Creating test prerequisites: network=" + networkName + ", dbVolume=" + dbVolumeName + ", dbContainer=" + dbContainerName + ", storeVolume=" + storeVolumeName);

        // Network + volumes
        client.createNetworkCmd().withName(networkName).exec();
        client.createVolumeCmd().withName(dbVolumeName).exec();
        client.createVolumeCmd().withName(storeVolumeName).exec();

        // Database container (duplicated on purpose, no dependency on StartDatabaseTaskHandler)
        Optional<Container> container = client.listContainersCmd().withShowAll(true).withNameFilter(List.of(dbContainerName)).exec().stream().findFirst();
        if (container.isEmpty()) {
            CreateContainerResponse response = client.createContainerCmd("postgres:latest")
                    .withName(dbContainerName)
                    .withHostName(dbContainerName)
                    .withEnv(
                            "POSTGRES_USER=test",
                            "POSTGRES_PASSWORD=test",
                            "POSTGRES_DB=test"
                    )
                    .withHostConfig(HostConfig.newHostConfig()
                            .withNetworkMode(networkName)
                            .withBinds(new Bind(dbVolumeName, new Volume("/var/lib/postgresql"))))
                    .exec();

            if (response.getId() == null) {
                fail("Failed to create database container in @BeforeAll");
                return;
            }
            client.startContainerCmd(response.getId()).exec();
        } else if (!container.get().getState().equalsIgnoreCase("running")) {
            client.startContainerCmd(container.get().getId()).exec();
        }
    }

    @AfterAll
    public void cleanup() {
        // Remove store + db containers
        List<Container> containers = client.listContainersCmd().withShowAll(true).exec();
        for (Container container : containers) {
            if (container.getNames() == null) continue;
            for (String name : container.getNames()) {
                if (name == null) continue;
                boolean isTest = name.startsWith("/" + STORE_CONTAINER_NAME_PREFIX) || name.startsWith("/" + DB_CONTAINER_NAME_PREFIX);
                if (!isTest) continue;

                LOGGER.log(Level.INFO, "Deleting container: " + name + " (id=" + container.getId() + ")");
                try {
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
                if (volume.getName() == null) continue;
                if (volume.getName().startsWith(DB_VOLUME_NAME_PREFIX) || volume.getName().startsWith(STORE_VOLUME_NAME_PREFIX)) {
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
        LOGGER.log(Level.INFO, "Testing bad context for start store...");
        assertThrows(TaskException.class, storeHandler::execute, "Expected TaskException for bad context");
        assertEquals(TaskStatus.FAILED, storeHandler.getStatus(), "Expected task status to be FAILED for bad context");
    }

    @Test
    @Transactional
    public void testStartStoreWithDatabase() throws TaskException {
        LOGGER.log(Level.INFO, "Testing starting store container with a database...");

        ProcessContext storeContext = new ProcessContext();
        storeContext.setValue(StartDockerStoreTask.NETWORK_NAME, networkName);
        // The image must exist locally for the test.
        storeContext.setValue(StartDockerStoreTask.STORE_IMAGE_NAME, STORE_IMAGE);
        storeContext.setValue(StartDockerStoreTask.STORE_NAME, "test");
        storeContext.setValue(StartDockerStoreTask.STORE_VOLUME_NAME, storeVolumeName);
        storeContext.setValue(StartDockerStoreTask.STORE_CONTAINER_NAME, storeContainerName);
        storeContext.setValue(StartDockerStoreTask.STORE_OWNER, "test");
        storeContext.setValue(StartDockerStoreTask.STORE_FQDN, "test.stores.local.test");
        storeContext.setValue(StartDockerStoreTask.STORE_TOPOLOGY_ENABLED, false);
        storeContext.setValue(StartDockerStoreTask.STORE_DB_CONTAINER_NAME, dbContainerName);
        storeContext.setValue(StartDockerStoreTask.STORE_DB_NAME, "test");
        storeContext.setValue(StartDockerStoreTask.STORE_DB_USER, "test");
        storeContext.setValue(StartDockerStoreTask.STORE_DB_PASSWORD, "test");

        storeHandler.setContext(storeContext);
        storeHandler.execute();

        assertEquals(TaskStatus.COMPLETED, storeHandler.getStatus(), "Expected store task status to be COMPLETED");
        assertTrue(storeHandler.getLog().contains("Store container"), "Expected log to mention store container");

        Optional<Container> created = client.listContainersCmd().withShowAll(true).exec().stream()
                .filter(c -> c.getNames() != null)
                .filter(c -> Arrays.asList(c.getNames()).contains(("/" + storeContainerName)))
                .findFirst();

        assertTrue(created.isPresent(), "Expected store container to exist after handler execution");

        // Ensure the Quarkus application is actually started.
        awaitStoreHealth(created.get().getId());
    }

    private String getContainerIp(String containerId, String network) {
        var inspect = client.inspectContainerCmd(containerId).exec();
        if (inspect.getNetworkSettings() == null || inspect.getNetworkSettings().getNetworks() == null) {
            throw new IllegalStateException("No network settings found for container " + containerId);
        }
        var net = inspect.getNetworkSettings().getNetworks().get(network);
        if (net == null || net.getIpAddress() == null || net.getIpAddress().isBlank()) {
            throw new IllegalStateException("No IP address found for container " + containerId + " on network " + network);
        }
        return net.getIpAddress();
    }

    private void awaitStoreHealth(String containerId) {
        String ip = getContainerIp(containerId, networkName);

        // Use the more common SmallRye Health endpoints.
        URI live = URI.create("http://" + ip + ":8080/q/health/live");
        URI ready = URI.create("http://" + ip + ":8080/q/health/ready");

        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

        long deadline = System.currentTimeMillis() + 30_000;
        Throwable lastError = null;

        while (System.currentTimeMillis() < deadline) {
            try {
                if (httpGet200(http, live) && httpGet200(http, ready)) {
                    return;
                }
            } catch (Exception e) {
                lastError = e;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while waiting for store health", e);
            }
        }

        String logs;
        try {
            logs = client.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withTail(200)
                    .exec(new com.github.dockerjava.api.async.ResultCallback.Adapter<>() {
                    })
                    .toString();
        } catch (Exception e) {
            logs = "<could not fetch docker logs: " + e.getMessage() + ">";
        }

        throw new AssertionError("Store health endpoint did not become ready within 30s: live=" + live + ", ready=" + ready + "\nLast error: " + lastError + "\nContainer logs (tail):\n" + logs, lastError);
    }

    private boolean httpGet200(HttpClient http, URI uri) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(2)).GET().build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 200) {
            return true;
        }
        throw new IllegalStateException("Unexpected status: " + res.statusCode() + " uri=" + uri + " body=" + res.body());
    }

}
