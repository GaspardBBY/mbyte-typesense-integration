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
package fr.jayblanc.mbyte.manager.core.runtime.command;

import fr.jayblanc.mbyte.manager.core.ApplicationCommand;
import fr.jayblanc.mbyte.manager.core.ApplicationStatus;
import fr.jayblanc.mbyte.manager.core.descriptor.DockerStoreDescriptor;
import fr.jayblanc.mbyte.manager.core.entity.Environment;
import fr.jayblanc.mbyte.manager.core.runtime.task.application.UpdateAppStatusTask;
import fr.jayblanc.mbyte.manager.core.runtime.task.database.StartDockerPgSQLTask;
import fr.jayblanc.mbyte.manager.core.runtime.task.network.CreateDockerNetworkTask;
import fr.jayblanc.mbyte.manager.core.runtime.task.store.StartDockerStoreTask;
import fr.jayblanc.mbyte.manager.core.runtime.task.volume.CreateDockerVolumeTask;
import fr.jayblanc.mbyte.manager.process.ProcessDefinition;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

/**
 * @author Jerome Blanchard
 */
@ApplicationScoped
public class StartDockerStoreCommand implements ApplicationCommand {

    public static final String NAME = "START";
    public static final int VERSION = 1;

    @Override
    public String forAppType() {
        return DockerStoreDescriptor.TYPE;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Integer getVersion() {
        return VERSION;
    }

    @Override
    public ProcessDefinition buildProcessDefinition(Environment env) {
        return ProcessDefinition.builder()
                .withAppId(env.getApp())
                .withName(forAppType() + "_" + getName() + "_V" + getVersion())
                .withEnvironment(env)
                .addTask(UpdateAppStatusTask.TASK_NAME, Map.of(UpdateAppStatusTask.APP_STATUS, ApplicationStatus.STARTING.name()))
                .addTask(CreateDockerNetworkTask.TASK_NAME, Map.of(CreateDockerNetworkTask.NETWORK_NAME, "$" + DockerStoreDescriptor.EnvKey.STORE_NETWORK_NAME ))
                .addTask(CreateDockerVolumeTask.TASK_NAME, Map.of(CreateDockerVolumeTask.VOLUME_NAME, "$" + DockerStoreDescriptor.EnvKey.STORE_DB_VOLUME_NAME ))
                .addTask(CreateDockerVolumeTask.TASK_NAME, Map.of(CreateDockerVolumeTask.VOLUME_NAME, "$" + DockerStoreDescriptor.EnvKey.STORE_VOLUME_NAME ))
                .addTask(StartDockerPgSQLTask.TASK_NAME, Map.of(
                        StartDockerPgSQLTask.NETWORK_NAME, "$" + DockerStoreDescriptor.EnvKey.STORE_NETWORK_NAME,
                        StartDockerPgSQLTask.DB_VOLUME_NAME, "$" + DockerStoreDescriptor.EnvKey.STORE_DB_VOLUME_NAME,
                        StartDockerPgSQLTask.DB_CONTAINER_NAME, "$" + DockerStoreDescriptor.EnvKey.STORE_DB_CONTAINER_NAME,
                        StartDockerPgSQLTask.DB_NAME, "$" + DockerStoreDescriptor.EnvKey.STORE_DB_NAME,
                        StartDockerPgSQLTask.DB_USER, "$" + DockerStoreDescriptor.EnvKey.STORE_DB_USER,
                        StartDockerPgSQLTask.DB_PASSWORD, "$" + DockerStoreDescriptor.EnvKey.STORE_DB_PASSWORD
                ))
                .addTask(StartDockerStoreTask.TASK_NAME)
                .addTask(UpdateAppStatusTask.TASK_NAME, Map.of(UpdateAppStatusTask.APP_STATUS, ApplicationStatus.STARTED.name()))
                .build();
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
