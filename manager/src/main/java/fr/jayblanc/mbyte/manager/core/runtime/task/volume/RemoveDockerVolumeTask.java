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
public class RemoveDockerVolumeTask extends Task {

    public static final String TASK_NAME = "RemoveDockerVolume";
    public static final String FAIL_IF_UNEXISTS = "FAIL_IF_UNEXISTS";
    public static final String VOLUME_NAME = "VOLUME_NAME";

    @Inject DockerClient client;

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    public void execute() throws TaskException {
        String volumeName = getMandatoryContextValue(VOLUME_NAME);
        boolean failIfUnexist = getContextValue(FAIL_IF_UNEXISTS, Boolean.class, false);

        Optional<InspectVolumeResponse> volume = client.listVolumesCmd().exec().getVolumes().stream().filter(v -> v.getName().equals(volumeName)).findFirst();
        if (volume.isPresent()) {
            client.removeVolumeCmd(volumeName).exec();
            this.complete(String.format("Volume removed for name: '%s' and mount point: '%s'", volume.get().getName(), volume.get().getMountpoint()));
        } else {
            if (failIfUnexist) {
                this.fail(String.format("Volume doest not exists with name: '%s'", volumeName));
                throw new TaskException("Docker volume does not exists for name: " + volumeName);
            }
            this.complete(String.format("Volume does not exists with name: '%s'", volumeName));
        }
    }

}
