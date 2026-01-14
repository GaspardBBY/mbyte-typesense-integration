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
import fr.jayblanc.mbyte.manager.process.TaskHandler;
import jakarta.inject.Inject;
import jakarta.transaction.TransactionScoped;

import java.util.Optional;

/**
 * @author Jerome Blanchard
 */
@TransactionScoped
public class RemoveNetworkTaskHandler extends TaskHandler {

    public static final String TASK_NAME = "RemoveNetwork";
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
        if (network.isPresent()) {
            if (network.get().getContainers() != null && !network.get().getContainers().isEmpty()) {
                this.fail(String.format("Cannot remove network with name: '%s' because it has connected containers", networkName));
                throw new TaskException("Network removal failed for name: " + networkName + " because it has connected containers");
            }
            client.removeNetworkCmd(network.get().getId()).exec();
            network = client.listNetworksCmd().withNameFilter(networkName).exec().stream().findFirst();
            if (network.isPresent()) {
                this.fail(String.format("Failed to remove network with name: '%s'", networkName));
                throw new TaskException("Network removal failed for name: " + networkName);
            } else {
                this.complete(String.format("Removed network with name: '%s'", networkName));
            }
        } else {
            this.complete(String.format("No network with name: '%s', nothing done", networkName));
        }
    }

}
