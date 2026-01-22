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
package fr.jayblanc.mbyte.manager.core.runtime.task.store;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import fr.jayblanc.mbyte.manager.process.TaskException;
import fr.jayblanc.mbyte.manager.process.Task;
import jakarta.inject.Inject;
import jakarta.transaction.TransactionScoped;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Jerome Blanchard
 */
@TransactionScoped
public class StartDockerStoreTask extends Task {

    public static final String TASK_NAME = "StartDockerStore";
    public static final String NETWORK_NAME = "STORE_NETWORK_NAME";
    public static final String STORE_IMAGE_NAME = "STORE_IMAGE_NAME";
    public static final String STORE_NAME = "STORE_NAME";
    public static final String STORE_VOLUME_NAME = "STORE_VOLUME_NAME";
    public static final String STORE_CONTAINER_NAME = "STORE_CONTAINER_NAME";
    public static final String STORE_OWNER = "STORE_OWNER";
    public static final String STORE_FQDN = "STORE_FQDN";
    public static final String STORE_TOPOLOGY_ENABLED = "STORE_TOPOLOGY_ENABLED";
    public static final String STORE_DB_CONTAINER_NAME = "STORE_DB_CONTAINER_NAME";
    public static final String STORE_DB_NAME = "STORE_DB_NAME";
    public static final String STORE_DB_USER = "STORE_DB_USER";
    public static final String STORE_DB_PASSWORD = "STORE_DB_PASSWORD";

    @Inject DockerClient client;

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    public void execute() throws TaskException {
        String networkName = getMandatoryContextValue(NETWORK_NAME);
        String storeImageName = getMandatoryContextValue(STORE_IMAGE_NAME);
        String storeVolumeName = getMandatoryContextValue(STORE_VOLUME_NAME);
        String storeContainerName = getMandatoryContextValue(STORE_CONTAINER_NAME);
        String storeName = getMandatoryContextValue(STORE_NAME);
        String storeOwner = getMandatoryContextValue(STORE_OWNER);
        String storeFqdn = getMandatoryContextValue(STORE_FQDN);
        boolean storeTopologyEnabled = getContextValue(STORE_TOPOLOGY_ENABLED, Boolean.class, true);
        String dbContainerName = getMandatoryContextValue(STORE_DB_CONTAINER_NAME);
        String dbName = getMandatoryContextValue(STORE_DB_NAME);
        String dbUser = getMandatoryContextValue(STORE_DB_USER);
        String dbPass = getMandatoryContextValue(STORE_DB_PASSWORD);

        Optional<Container> container = client.listContainersCmd().withNameFilter(List.of(storeContainerName)).exec().stream().findFirst();
        if (container.isEmpty()) {
            CreateContainerResponse response = client.createContainerCmd(storeImageName)
                    .withName(storeContainerName)
                    .withHostName(storeContainerName)
                    .withEnv(
                            "QUARKUS_HTTP_PORT=8080",
                            "STORE.ROOT=/home/jboss",
                            "STORE.AUTH.OWNER=" + storeOwner,
                            "STORE.TOPOLOGY.ENABLED=" + storeTopologyEnabled,
                            "STORE.TOPOLOGY.HOST=consul",
                            "STORE.TOPOLOGY.PORT=8500",
                            "STORE.TOPOLOGY.SERVICE.HOST=" + storeFqdn,
                            "QUARKUS.DATASOURCE.USERNAME=" + dbUser,
                            "QUARKUS.DATASOURCE.PASSWORD=" + dbPass,
                            "QUARKUS.DATASOURCE.JDBC.URL=jdbc:postgresql://" + dbContainerName + ":5432/" + dbName
                    )
                    .withLabels(Map.of(
                            "traefik.enable", "true",
                            "traefik.docker.network", "mbyte",
                            "traefik.http.routers." + storeOwner + ".rule", "Host(`" + storeFqdn + "`)",
                            "traefik.http.routers." + storeOwner + ".entrypoints", "http",
                            "traefik.http.routers." + storeOwner + ".service", storeOwner + "-http",
                            "traefik.http.services." + storeOwner + "-http.loadbalancer.server.port","8080"
                    ))
                    .withHostConfig(HostConfig.newHostConfig()
                            .withNetworkMode(networkName)
                            .withBinds(new Bind(storeVolumeName, new Volume("/home/jboss"))))
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
