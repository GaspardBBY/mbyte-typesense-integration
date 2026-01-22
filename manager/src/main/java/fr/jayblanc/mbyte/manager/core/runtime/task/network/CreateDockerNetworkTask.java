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
package fr.jayblanc.mbyte.manager.core.runtime.task.network;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateNetworkResponse;
import com.github.dockerjava.api.model.Network;
import fr.jayblanc.mbyte.manager.process.TaskException;
import fr.jayblanc.mbyte.manager.process.Task;
import jakarta.inject.Inject;
import jakarta.transaction.TransactionScoped;

import java.util.Optional;

/**
 * @author Jerome Blanchard
 */
@TransactionScoped
public class CreateDockerNetworkTask extends Task {

    public static final String TASK_NAME = "CreateDockerNetwork";
    public static final String NETWORK_NAME = "NETWORK_NAME";

    @Inject DockerClient client;

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    public void execute() throws TaskException {
        String networkName = getMandatoryContextValue(NETWORK_NAME);

        Optional<Network> network = client.listNetworksCmd().withNameFilter(networkName).exec().stream().findFirst();
        if (network.isEmpty()) {
            CreateNetworkResponse response = client.createNetworkCmd().withName(networkName).exec();
            if (response.getId() == null) {
                this.fail(String.format("Failed to create network with name: '%s'", networkName));
                throw new TaskException("Network creation failed for name: " + networkName);
            } else {
                this.complete(String.format("Network created with name: '%s', id: '%s'", networkName, response.getId()));
            }
        } else {
            this.complete(String.format("Found existing network with name: '%s', id: '%s'", network.get().getName(), network.get().getId()));
        }
    }

}
