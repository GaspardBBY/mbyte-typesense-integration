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
package fr.jayblanc.mbyte.manager.core.runtime.task.volume;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateVolumeResponse;
import com.github.dockerjava.api.command.InspectVolumeResponse;
import fr.jayblanc.mbyte.manager.process.TaskException;
import fr.jayblanc.mbyte.manager.process.Task;
import jakarta.inject.Inject;
import jakarta.transaction.TransactionScoped;

import java.util.Optional;

/**
 * @author Jerome Blanchard
 */
@TransactionScoped
public class CreateDockerVolumeTask extends Task {

    public static final String TASK_NAME = "CreateDockerVolume";
    public static final String FAIL_IF_EXISTS = "FAIL_IF_EXISTS";
    public static final String VOLUME_NAME = "VOLUME_NAME";

    @Inject DockerClient client;

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    public void execute() throws TaskException {
        String volumeName = getMandatoryContextValue(VOLUME_NAME);
        boolean failIfExists = getContextValue(FAIL_IF_EXISTS, Boolean.class, false);

        Optional<InspectVolumeResponse> volume = client.listVolumesCmd().exec().getVolumes().stream().filter(v -> v.getName().equals(volumeName)).findFirst();
        if (volume.isEmpty()) {
            CreateVolumeResponse response = client.createVolumeCmd().withName(volumeName).exec();
            this.complete(String.format("Volume created with name: '%s' and mount point: '%s'", response.getName(), response.getMountpoint()));
        } else {
            if (failIfExists) {
                this.fail(String.format("Volume already exists with name: '%s'", volume.get().getName()));
                throw new TaskException("Volume already exists for name: " + volumeName);
            }
            this.complete(String.format("Found existing volume with name: '%s'", volume.get().getName()));
        }
    }

}
