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
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import fr.jayblanc.mbyte.manager.process.TaskException;
import fr.jayblanc.mbyte.manager.process.TaskHandler;
import jakarta.inject.Inject;
import jakarta.transaction.TransactionScoped;

import java.util.List;
import java.util.Optional;

/**
 * @author Jerome Blanchard
 */
@TransactionScoped
public class StartDatabaseTaskHandler extends TaskHandler {

    public static final String TASK_NAME = "StartDatabase";
    public static final String NETWORK_NAME = "NETWORK_NAME";
    public static final String DB_VOLUME_NAME = "DB_VOLUME_NAME";
    public static final String DB_CONTAINER_NAME = "DB_CONTAINER_NAME";
    public static final String DB_USER = "DB_USER";
    public static final String DB_PASSWORD = "DB_PASSWORD";

    @Inject DockerClient client;

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    public void execute() throws TaskException {
        String networkName = getMandatoryContextValue(NETWORK_NAME);
        String dbVolumeName = getMandatoryContextValue(DB_VOLUME_NAME);
        String dbContainerName = getMandatoryContextValue(DB_CONTAINER_NAME);
        String dbUser = getMandatoryContextValue(DB_USER);
        String dbPass = getMandatoryContextValue(DB_PASSWORD);

        Optional<Container> container = client.listContainersCmd().withNameFilter(List.of(DB_CONTAINER_NAME)).exec().stream().findFirst();
        if (container.isEmpty()) {
            CreateContainerResponse response = client.createContainerCmd("postgres:latest")
                    .withName(dbContainerName)
                    .withHostName(dbContainerName)
                    .withEnv(
                            "POSTGRES_USER=" + dbUser,
                            "POSTGRES_PASSWORD=" + dbPass,
                            "POSTGRES_DB=store"
                    )
                    .withHostConfig(HostConfig.newHostConfig()
                            .withNetworkMode(networkName)
                            .withBinds(new Bind(dbVolumeName, new Volume("/var/lib/postgresql"))))
                    .exec();
            if (response.getId() == null) {
                this.fail(String.format("Failed to create database container with name: '%s'", dbContainerName));
                throw new TaskException("Database container creation failed for name: " + dbContainerName);
            }
            client.startContainerCmd(response.getId()).exec();
            this.complete(String.format("Database container started with id: '%s'", response.getId()));
        } else {
            this.log(String.format("Found existing database container with name: '%s', id: '%s'", container.get().getNames()[0], container.get().getId()));
            if (!container.get().getState().equalsIgnoreCase("running")) {
                client.startContainerCmd(container.get().getId()).exec();
                this.complete(String.format("Database container started with id: '%s'", container.get().getId()));
            } else {
                this.complete(String.format("Database container is already running with id: '%s'", container.get().getId()));
            }
        }

    }

}
