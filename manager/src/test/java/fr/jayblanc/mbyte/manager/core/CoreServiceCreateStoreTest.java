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
package fr.jayblanc.mbyte.manager.core;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectVolumeResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Network;
import fr.jayblanc.mbyte.manager.core.descriptor.DockerStoreDescriptor;
import fr.jayblanc.mbyte.manager.core.entity.Application;
import fr.jayblanc.mbyte.manager.core.entity.EnvironmentEntry;
import fr.jayblanc.mbyte.manager.core.runtime.command.StartDockerStoreCommand;
import fr.jayblanc.mbyte.manager.process.ProcessEngine;
import fr.jayblanc.mbyte.manager.process.entity.Process;
import fr.jayblanc.mbyte.manager.process.entity.ProcessStatus;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.UserTransaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class CoreServiceCreateStoreTest {

    @Inject CoreConfig config;
    @Inject CoreService core;
    @Inject CoreServiceAdmin admin;
    @Inject ProcessEngine engine;
    @Inject DockerClient docker;
    @Inject UserTransaction userTransaction;

    @AfterEach
    public void cleanup() {
        String realm = config.instance();

        // Best-effort cleanup of test-created containers/networks.
        // We don't have the storeId here reliably if the test failed early, so we only cleanup by prefix.
        List<Container> containers = docker.listContainersCmd().withShowAll(true).exec();
        for (Container container : containers) {
            if (container.getNames() == null) continue;
            boolean match = java.util.Arrays.stream(container.getNames())
                    .filter(Objects::nonNull)
                    .map(n -> n.replaceFirst("^/", ""))
                    .anyMatch(n -> n.startsWith(realm));
            if (!match) continue;
            try {
                try { docker.stopContainerCmd(container.getId()).exec(); } catch (RuntimeException ignored) {}
                docker.removeContainerCmd(container.getId()).withForce(true).withRemoveVolumes(true).exec();
            } catch (RuntimeException ignored) {
            }
        }

        List<InspectVolumeResponse> volumeResponses = docker.listVolumesCmd().exec().getVolumes();
        for (InspectVolumeResponse volumeResponse : volumeResponses) {
            if (volumeResponse.getName() != null && volumeResponse.getName().startsWith(realm)) {
                try {
                    docker.removeVolumeCmd(volumeResponse.getName()).exec();
                } catch (RuntimeException ignored) {
                }
            }
        }

        List<Network> networks = docker.listNetworksCmd().exec();
        for (Network net : networks) {
            if (net.getName() != null && net.getName().startsWith(realm)) {
                try {
                    docker.removeNetworkCmd(net.getId()).exec();
                } catch (RuntimeException ignored) {
                }
            }
        }
    }

    @Test
    public void testCreateStoreApp() throws Exception {
        String appName = "test-store";

        userTransaction.begin();
        String appId = core.createApp(DockerStoreDescriptor.TYPE, appName);
        userTransaction.commit();
        assertNotNull(appId);

        userTransaction.begin();
        core.updateAppEnv(appId, Set.of(EnvironmentEntry.of(DockerStoreDescriptor.EnvKey.STORE_TOPOLOGY_ENABLED.name(), false)));
        userTransaction.commit();

        // Ensure app exists
        Application app = core.getApp(appId);
        assertEquals(appName, app.getName());
        assertEquals(ApplicationStatus.CREATED, app.getStatus(), "Initial app status should be CREATED");

        // Call the start command on the app
        userTransaction.begin();
        String procId = core.runAppCommand(appId, StartDockerStoreCommand.NAME, java.util.Collections.emptyMap());
        userTransaction.commit();

        // Wait for the process to complete and update store status
        long start = System.currentTimeMillis();
        List<Process> procs = engine.findAllProcessesForApp(appId);
        assertEquals(1, procs.size(), "Expected exactly one process for the created store");
        Process proc = engine.findRunningProcessesForApp(appId).stream().findFirst().orElse(null);
        assertNotNull(proc, "Expected a running process for the created store");
        Process procById = engine.getProcess(procId);
        assertEquals(proc.getId(), procById.getId(), "Process fetched by ID should match the running process");
        assertEquals(proc.getAppId(), procById.getAppId(), "Process app ID should match");
        assertEquals(proc.getCreationDate(), procById.getCreationDate(), "Process creation date should match");

        while (!procById.isFinished()) {
            Thread.sleep(1000);
            if ((System.currentTimeMillis() - start) > 60_000) {
                fail("Timed out waiting for application 'test-store' creation process to complete");
            }
            procById = engine.getProcess(procId);
        }

        assertEquals(ProcessStatus.COMPLETED, procById.getStatus(), "Expected process status to be COMPLETED");

        // Store should now be available.
        Application updatedApp = admin.systemGetApp(appId);
        assertEquals(ApplicationStatus.STARTED, updatedApp.getStatus(), "Expected application 'test-store' status to be STARTED after process completion");

        // Sanity check: store container should exist.
        String networkName = (String) core.getAppEnv(appId).get(DockerStoreDescriptor.EnvKey.STORE_NETWORK_NAME.name()).getValue();
        String storeContainerName = (String) core.getAppEnv(appId).get(DockerStoreDescriptor.EnvKey.STORE_CONTAINER_NAME.name()).getValue();
        List<Container> containers = docker.listContainersCmd().withShowAll(true).exec();
        Optional<Container> created = containers.stream().filter(c -> c.getNames() != null && Arrays.stream(c.getNames())
                    .map(n -> n == null ? null : n.replaceFirst("^/", ""))
                    .anyMatch(storeContainerName::equals)).findFirst();
        assertTrue(created.isPresent(), "Expected a Docker container for the store");

        awaitStoreHealth(created.get().getId(), networkName);
    }

    private String getContainerIp(String containerId, String network) {
        var inspect = docker.inspectContainerCmd(containerId).exec();
        if (inspect.getNetworkSettings() == null || inspect.getNetworkSettings().getNetworks() == null) {
            throw new IllegalStateException("No network settings found for container " + containerId);
        }
        var net = inspect.getNetworkSettings().getNetworks().get(network);
        if (net == null || net.getIpAddress() == null || net.getIpAddress().isBlank()) {
            throw new IllegalStateException("No IP address found for container " + containerId + " on network " + network);
        }
        return net.getIpAddress();
    }

    private void awaitStoreHealth(String containerId, String networkName) {
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
            logs = docker.logContainerCmd(containerId)
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
