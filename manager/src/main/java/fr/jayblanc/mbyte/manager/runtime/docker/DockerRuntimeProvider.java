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
package fr.jayblanc.mbyte.manager.runtime.docker;

import com.github.dockerjava.api.DockerClient;
import fr.jayblanc.mbyte.manager.core.entity.Store;
import fr.jayblanc.mbyte.manager.process.ProcessAlreadyRunningException;
import fr.jayblanc.mbyte.manager.runtime.docker.task.*;
import fr.jayblanc.mbyte.manager.process.ProcessDefinition;
import fr.jayblanc.mbyte.manager.runtime.RuntimeProvider;
import fr.jayblanc.mbyte.manager.runtime.RuntimeProviderException;
import fr.jayblanc.mbyte.manager.process.ProcessEngine;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Singleton
public class DockerRuntimeProvider implements RuntimeProvider {

    private static final Logger LOGGER = Logger.getLogger(DockerRuntimeProvider.class.getName());
    private static final String NAME = "docker";

    private static final String STORE_SUFFIX = ".store";
    private static final String DB_SUFFIX = ".db";
    private static final String VOLUME_SUFFIX = ".vol";

    @Inject DockerClient client;
    @Inject DockerStoreProviderConfig config;
    @Inject ProcessEngine engine;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public List<String> listAllStores() {
        LOGGER.log(Level.INFO, "Listing store apps");
        return client.listContainersCmd().exec().stream().map(container -> String.join("", container.getNames()) + " / " + container.getImage()).collect(Collectors.toList());
    }

    @Override
    public String startStore(String id, String name, String owner) throws ProcessAlreadyRunningException {
        ProcessDefinition createStoreProcess = ProcessDefinition.builder()
            .withName("Create Docker Store for id: " + id)
            .forStore(id)
            .setGlobalContextEntry(CreateNetworkTaskHandler.NETWORK_NAME, config.instanceName().concat(".").concat("net"))
            .setGlobalContextEntry(StartDatabaseTaskHandler.DB_VOLUME_NAME, config.instanceName().concat(".").concat(id).concat(DB_SUFFIX).concat(VOLUME_SUFFIX))
            .setGlobalContextEntry(StartDatabaseTaskHandler.DB_CONTAINER_NAME, config.instanceName().concat(id).concat(DB_SUFFIX))
            .setGlobalContextEntry(StartDatabaseTaskHandler.DB_USER, owner)
            .setGlobalContextEntry(StartDatabaseTaskHandler.DB_PASSWORD, "Pp@asSw#".concat(id).concat("#W0orRdD!")) //TODO the password generation should be improved and stored in the Store secrets using a dedicated task
            .setGlobalContextEntry(StartStoreTaskHandler.STORE_IMAGE_NAME, "jerome/store:25-1-SNAPSHOT")
            .setGlobalContextEntry(StartStoreTaskHandler.STORE_NAME, name)
            .setGlobalContextEntry(StartStoreTaskHandler.STORE_DATA_VOLUME_NAME, config.instanceName().concat(".").concat(id).concat(STORE_SUFFIX).concat(VOLUME_SUFFIX))
            .setGlobalContextEntry(StartStoreTaskHandler.STORE_CONTAINER_NAME, config.instanceName().concat(".").concat(id).concat(STORE_SUFFIX))
            .setGlobalContextEntry(StartStoreTaskHandler.STORE_OWNER, owner)
            .setGlobalContextEntry(StartStoreTaskHandler.STORE_FQDN_SUFFIX, id.concat(".s.").concat(config.instanceName()))
            .addTask(UpdateStoreStatusTaskHandler.TASK_NAME)
            .setTaskContextEntry(UpdateStoreStatusTaskHandler.STORE_ID, id)
            .setTaskContextEntry(UpdateStoreStatusTaskHandler.STORE_STATUS, Store.Status.STARTING)
            .addTask(CreateNetworkTaskHandler.TASK_NAME)
            .addTask(CreateVolumeTaskHandler.TASK_NAME)
            .setTaskContextEntry(CreateVolumeTaskHandler.VOLUME_NAME, config.instanceName().concat(id).concat(DB_SUFFIX))
            .addTask(CreateVolumeTaskHandler.TASK_NAME)
            .setTaskContextEntry(CreateVolumeTaskHandler.VOLUME_NAME, config.instanceName().concat(id).concat(STORE_SUFFIX))
            .addTask(StartDatabaseTaskHandler.TASK_NAME)
            .addTask(StartStoreTaskHandler.TASK_NAME)
            .addTask(UpdateStoreStatusTaskHandler.TASK_NAME)
            .setTaskContextEntry(UpdateStoreStatusTaskHandler.STORE_ID, id)
            .setTaskContextEntry(UpdateStoreStatusTaskHandler.STORE_STATUS, Store.Status.AVAILABLE)
            .build();
        return engine.startProcess(createStoreProcess);
    }

    @Override
    public String stopStore(String id) throws RuntimeProviderException {
        throw new RuntimeProviderException("Not implemented yet");
    }

    @Override
    public String destroyStore(String id) throws RuntimeProviderException {
        throw new RuntimeProviderException("Not implemented yet");
    }


    /*
    LOGGER.log(Level.INFO, "Starting new store creation...");
    StringBuilder creationLog = new StringBuilder();

    // Step 1: load network 'mbyte.net'
    Optional<Network> network = client.listNetworksCmd().withNameFilter(config.network_name()).exec().stream().findFirst();
    if (network.isEmpty()) {
        LOGGER.log(Level.SEVERE, config.network_name() + " network not found, cannot create store app");
        creationLog.append("[Step 1/7] -FAILED- ").append(config.network_name()).append(" network not found, cannot create store app");
        return creationLog.toString();
    }
    LOGGER.log(Level.INFO, "Found existing network, name: " + network.get().getName() + ", id:" + network.get().getId());
    creationLog.append("[Step 1/7] -COMPLETED- ").append("Found existing network, name:").append(network.get().getName()).append(", id:").append(network.get().getId()).append("\n");

    // Step 2: create db volume 'mbyte.UUID.db.volume'
    String dbVolumeName = config.instanceName().concat(id).concat(DB_SUFFIX).concat(VOLUME_SUFFIX);
    Path dbLocalVolumePath =  Paths.get(config.workdir().local(), id, STORES_DB_PATH_SEGMENT);
    Path dbHostVolumePath =  Paths.get(config.workdir().host(), id, STORES_DB_PATH_SEGMENT);
    try {
        Files.createDirectories(dbLocalVolumePath);
        LOGGER.log(Level.INFO, "Created directories for db volume: " + dbLocalVolumePath);
        creationLog.append("[Step 2/7] -PROGRESS- ").append("Created directories for db volume: ").append(dbLocalVolumePath).append("\n");
    } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Failed to create directories for store db volume: " + dbLocalVolumePath, e);
        creationLog.append("[Step 2/7] -FAILED- ").append("Failed to create directories for store db volume: ").append(dbLocalVolumePath).append("\n");
        return creationLog.toString();
    }
    Optional<InspectVolumeResponse> dbVolume = client.listVolumesCmd().exec().getVolumes().stream()
        .filter(v -> dbVolumeName.equals(v.getName()))
        .findFirst();
    if (dbVolume.isEmpty()) {
        CreateVolumeResponse response = client.createVolumeCmd()
            .withName(dbVolumeName)
            .withDriver("local")
            .withDriverOpts(Map.of("type", "none", "o", "bind","device", dbHostVolumePath.toString()))
            .exec();
        LOGGER.log(Level.INFO, "Database volume created: " + response.getName());
        creationLog.append("[Step 2/7] -COMPLETED- ").append("Database volume created: ").append(response.getName()).append("\n");
    } else {
        LOGGER.log(Level.INFO, "Found existing database volume: " + dbVolume.get().getName());
        creationLog.append("[Step 2/7] -COMPLETED- ").append("Found existing database volume: ").append(dbVolume.get().getName()).append("\n");
    }

    // Step 3: create db container 'mbyte.UUID.db.cont'
    String dbContainerName = config.instanceName().concat(id).concat(DB_SUFFIX).concat(CONTAINER_SUFFIX);
    String dbContainerPassword = "Pp@asSw#".concat(id).concat("#W0orRdD!");
    CreateContainerResponse dbContainer = client.createContainerCmd("postgres:latest")
        .withName(dbContainerName)
        .withHostName(dbContainerName)
        .withEnv(
            "POSTGRES_USER=" + id,
            "POSTGRES_PASSWORD=" + dbContainerPassword,
            "POSTGRES_DB=store"
        )
        .withHostConfig(HostConfig.newHostConfig()
            .withNetworkMode(config.network_name())
            .withBinds(new Bind(dbVolumeName, new Volume("/var/lib/postgresql/data"))))
        .exec();
    LOGGER.log(Level.INFO, "Database container created for store: " + dbContainer.getId());
    creationLog.append("[Step 3/7] -COMPLETED- ").append("Database container created for store: ").append(dbContainer.getId()).append("\n");

    // Step 4: start db container
    client.startContainerCmd(dbContainer.getId()).exec();
    LOGGER.log(Level.INFO, "Database container started for store: " + dbContainer.getId());
    creationLog.append("[Step 4/7] -COMPLETED- ").append("Database container started for store: ").append(dbContainer.getId()).append("\n");

    // Step 5: create data volume 'mbyte.UUID.data.volume'
    String dataVolumeName = config.instanceName().concat(id).concat(DATA_SUFFIX).concat(VOLUME_SUFFIX);
    Path dataLocalVolumePath =  Paths.get(config.workdir().local(), id, STORES_DATA_PATH_SEGMENT);
    Path dataHostVolumePath =  Paths.get(config.workdir().host(), id, STORES_DATA_PATH_SEGMENT);
    try {
        Files.createDirectories(dataLocalVolumePath);
        LOGGER.log(Level.INFO, "Created directories for data volume: " + dataLocalVolumePath);
        creationLog.append("[Step 5/7] -PROGRESS- ").append("Created directories for data volume: ").append(dataLocalVolumePath).append("\n");
    } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Failed to create directories for store data volume: " + dataLocalVolumePath, e);
        creationLog.append("[Step 5/7] -FAILED- ").append("Failed to create directories for store data volume: ").append(dataLocalVolumePath).append("\n");
        return creationLog.toString();
    }
    Optional<InspectVolumeResponse> dataVolume = client.listVolumesCmd().exec().getVolumes().stream()
            .filter(v -> dataVolumeName.equals(v.getName()))
            .findFirst();
    if (dataVolume.isEmpty()) {
        CreateVolumeResponse response = client.createVolumeCmd()
                .withName(dataVolumeName)
                .withDriver("local")
                .withDriverOpts(Map.of("type", "none", "o", "bind","device", dataHostVolumePath.toString()))
                .exec();
        LOGGER.log(Level.INFO, "Data volume created: " + response.getName());
        creationLog.append("[Step 5/7] -COMPLETED- ").append("Data volume created: ").append(response.getName()).append("\n");
    } else {
        LOGGER.log(Level.INFO, "Found existing data volume: " + dataVolume.get().getName());
        creationLog.append("[Step 5/7] -COMPLETED- ").append("Found existing data volume: ").append(dataVolume.get().getName()).append("\n");
    }

    // Step 6: create store container 'mbyte.UUID.store.cont'
    String storeContainerName = config.instanceName().concat(id).concat(STORE_SUFFIX).concat(CONTAINER_SUFFIX);
    CreateContainerResponse storeContainer = client.createContainerCmd(config.image())
            .withName(storeContainerName)
            .withHostName(storeContainerName)
            .withEnv(
                    "QUARKUS_HTTP_PORT=8080",
                    "STORE.ROOT=/home/jboss",
                    "STORE.AUTH.OWNER=" + owner,
                    "STORE.TOPOLOGY.HOST=consul",
                    "STORE.TOPOLOGY.PORT=8500",
                    "STORE.TOPOLOGY.SERVICE.HOST=" + name + ".stores.mbyte.fr",
                    "QUARKUS.DATASOURCE.USERNAME=" + id,
                    "QUARKUS.DATASOURCE.PASSWORD=" + dbContainerPassword,
                    "QUARKUS.DATASOURCE.JDBC.URL=jdbc:postgresql://" + dbContainerName + ":5432/store"
            )
            .withLabels(Map.of(
                    "traefik.enable", "true",
                    "traefik.docker.network", "mbyte",
                    "traefik.http.routers." + id + ".rule", "Host(`" + name + ".stores.mbyte.fr`)",
                    "traefik.http.routers." + id + ".entrypoints", "http",
                    "traefik.http.routers." + id + ".service", id + "-http",
                    "traefik.http.services." + id + "-http.loadbalancer.server.port","8080"
            ))
            .withHostConfig(HostConfig.newHostConfig()
                    .withNetworkMode(config.network_name())
                    .withBinds(new Bind(dataVolumeName, new Volume("/home/jboss"))))
            .exec();
    LOGGER.log(Level.INFO, "Store container created: " + storeContainer.getId());
    creationLog.append("[Step 6/7] -COMPLETED- ").append("Store container created: ").append(storeContainer.getId()).append("\n");

    // Step 7: start store container
    client.startContainerCmd(storeContainer.getId()).exec();
    LOGGER.log(Level.INFO, "Store container started for store: " + storeContainer.getId());
    creationLog.append("[Step 7/7] -COMPLETED- ").append("Store container started for id: ").append(storeContainer.getId()).append("\n");

    return id;
    */
}
