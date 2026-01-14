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
import java.util.Map;
import java.util.Optional;

/**
 * @author Jerome Blanchard
 */
@TransactionScoped
public class StartStoreTaskHandler extends TaskHandler {

    public static final String TASK_NAME = "StartStore";
    public static final String NETWORK_NAME = "NETWORK_NAME";
    public static final String STORE_IMAGE_NAME = "STORE_IMAGE_NAME";
    public static final String STORE_NAME = "STORE_NAME";
    public static final String STORE_DATA_VOLUME_NAME = "STORE_DATA_VOLUME_NAME";
    public static final String STORE_CONTAINER_NAME = "STORE_CONTAINER_NAME";
    public static final String STORE_OWNER = "STORE_OWNER";
    public static final String STORE_FQDN_SUFFIX = "STORE_FQDN_SUFFIX";
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
        String storeImageName = getMandatoryContextValue(STORE_IMAGE_NAME);
        String storeDataVolumeName = getMandatoryContextValue(STORE_DATA_VOLUME_NAME);
        String storeContainerName = getMandatoryContextValue(STORE_CONTAINER_NAME);
        String storeName = getMandatoryContextValue(STORE_NAME);
        String storeOwner = getMandatoryContextValue(STORE_OWNER);
        String storeFqdnSuffix = getMandatoryContextValue(STORE_FQDN_SUFFIX);
        String dbContainerName = getMandatoryContextValue(DB_CONTAINER_NAME);
        String dbUser = getMandatoryContextValue(DB_USER);
        String dbPass = getMandatoryContextValue(DB_PASSWORD);

        Optional<Container> container = client.listContainersCmd().withNameFilter(List.of(storeContainerName)).exec().stream().findFirst();
        if (container.isEmpty()) {
            CreateContainerResponse response = client.createContainerCmd(storeImageName)
                    .withName(storeContainerName)
                    .withHostName(storeContainerName)
                    .withEnv(
                            "QUARKUS_HTTP_PORT=8080",
                            "STORE.ROOT=/home/jboss",
                            "STORE.AUTH.OWNER=" + storeOwner,
                            "STORE.TOPOLOGY.HOST=consul",
                            "STORE.TOPOLOGY.PORT=8500",
                            "STORE.TOPOLOGY.SERVICE.HOST=" + storeName.concat(".").concat(storeFqdnSuffix),
                            "QUARKUS.DATASOURCE.USERNAME=" + dbUser,
                            "QUARKUS.DATASOURCE.PASSWORD=" + dbPass,
                            "QUARKUS.DATASOURCE.JDBC.URL=jdbc:postgresql://" + dbContainerName + ":5432/store"
                    )
                    .withLabels(Map.of(
                            "traefik.enable", "true",
                            "traefik.docker.network", "mbyte",
                            "traefik.http.routers." + storeOwner + ".rule", "Host(`" + storeName.concat(".").concat(storeFqdnSuffix) + "`)",
                            "traefik.http.routers." + storeOwner + ".entrypoints", "http",
                            "traefik.http.routers." + storeOwner + ".service", storeOwner + "-http",
                            "traefik.http.services." + storeOwner + "-http.loadbalancer.server.port","8080"
                    ))
                    .withHostConfig(HostConfig.newHostConfig()
                            .withNetworkMode(networkName)
                            .withBinds(new Bind(storeDataVolumeName, new Volume("/home/jboss"))))
                    .exec();
            if (response.getId() == null) {
                this.fail(String.format("Failed to create store container with name: '%s'", storeContainerName));
                throw new TaskException("Store container creation failed for name: " + storeContainerName);
            }
            client.startContainerCmd(response.getId()).exec();
            this.complete(String.format("Store container started with id: '%s'", response.getId()));
        } else {
            this.log(String.format("Found existing store container with name: '%s', id: '%s'", container.get().getNames()[0], container.get().getId()));
            if (!container.get().getState().equalsIgnoreCase("running")) {
                client.startContainerCmd(container.get().getId()).exec();
                this.complete(String.format("Store container started with id: '%s'", container.get().getId()));
            } else {
                this.complete(String.format("Store container is already running with id: '%s'", container.get().getId()));
            }
        }

    }

}
