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
import com.github.dockerjava.api.command.InspectVolumeResponse;
import fr.jayblanc.mbyte.manager.core.runtime.task.volume.CreateDockerVolumeTask;
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
public class CreateDockerVolumeTaskTest {

    private static final Logger LOGGER = Logger.getLogger(CreateDockerVolumeTaskTest.class.getName());
    private static final String VOLUME_NAME_PREFIX = "test_create_docker_volume_";

    @Inject CreateDockerVolumeTask handler;
    @Inject DockerClient client;

    @AfterEach
    public void cleanupVolumes() {
        List<InspectVolumeResponse> volumes = client.listVolumesCmd().exec().getVolumes();
        for (InspectVolumeResponse volume : volumes) {
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

    @Test
    @Transactional
    public void testBadContext() {
        LOGGER.log(Level.INFO, "Testing bad context for volume create...");
        assertThrows(TaskException.class, handler::execute, "Expected TaskException for bad context");
        assertEquals(TaskStatus.FAILED, handler.getStatus(), "Expected task status to be FAILED for bad context");
    }

    @Test
    @Transactional
    public void testCreateExistingVolume() throws TaskException {
        LOGGER.log(Level.INFO, "Testing create existing volume...");

        String volumeName = VOLUME_NAME_PREFIX + System.currentTimeMillis();
        client.createVolumeCmd().withName(volumeName).exec();

        ProcessContext context = new ProcessContext();
        context.setValue(CreateDockerVolumeTask.VOLUME_NAME, volumeName);
        context.setValue(CreateDockerVolumeTask.FAIL_IF_EXISTS, false);
        handler.setContext(context);

        handler.execute();

        assertTrue(handler.getLog().contains("Found existing volume with name: '" + volumeName + "'"), "Expected log to contain success message for already existing volume");
        assertEquals(TaskStatus.COMPLETED, handler.getStatus(), "Expected task status to be COMPLETED for already existing volume");
    }

    @Test
    @Transactional
    public void testCreateVolumeFailIfExists() {
        LOGGER.log(Level.INFO, "Testing create volume with FAIL_IF_EXISTS=true...");

        String volumeName = VOLUME_NAME_PREFIX + System.currentTimeMillis();
        client.createVolumeCmd().withName(volumeName).exec();

        ProcessContext context = new ProcessContext();
        context.setValue(CreateDockerVolumeTask.VOLUME_NAME, volumeName);
        context.setValue(CreateDockerVolumeTask.FAIL_IF_EXISTS, true);
        handler.setContext(context);

        assertThrows(TaskException.class, handler::execute, "Expected TaskException when volume exists and FAIL_IF_EXISTS=true");
        assertEquals(TaskStatus.FAILED, handler.getStatus(), "Expected task status to be FAILED when volume exists and FAIL_IF_EXISTS=true");
        assertTrue(handler.getLog().contains("Volume already exists with name: '" + volumeName + "'"), "Expected log to contain failure message for already existing volume");
    }

    @Test
    @Transactional
    public void testCreateVolume() throws TaskException {
        LOGGER.log(Level.INFO, "Testing creating new volume...");
        ProcessContext context = new ProcessContext();
        String volumeName = VOLUME_NAME_PREFIX + System.currentTimeMillis();
        context.setValue(CreateDockerVolumeTask.VOLUME_NAME, volumeName);
        handler.setContext(context);

        handler.execute();

        assertTrue(handler.getLog().contains("Volume created with name: '" + volumeName + "'"), "Expected log to contain success message for created volume");
        assertEquals(TaskStatus.COMPLETED, handler.getStatus(), "Expected task status to be COMPLETED for created volume");

        boolean created = client.listVolumesCmd().exec().getVolumes().stream().anyMatch(v -> volumeName.equals(v.getName()));
        assertTrue(created, "Expected docker volume to exist after creation");
    }
}

