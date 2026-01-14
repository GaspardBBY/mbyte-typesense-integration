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
package fr.jayblanc.mbyte.manager.topology;

import com.google.common.net.HostAndPort;
import com.orbitz.consul.CatalogClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.NotRegisteredException;
import com.orbitz.consul.model.agent.ImmutableRegCheck;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import com.orbitz.consul.model.catalog.CatalogService;
import io.quarkus.runtime.Startup;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
public class TopologyServiceBean implements TopologyService {

    private static final Logger LOGGER = Logger.getLogger(TopologyServiceBean.class.getName());

    @Inject TopologyConfig config;

    private volatile Consul consulClient;
    private volatile String serviceName;
    private volatile String instanceId;
    private volatile boolean registered = false;

    @PostConstruct
    public void init() {
        if (!config.enabled()) {
            LOGGER.log(Level.INFO, "Topology service is disabled");
            return;
        }
        LOGGER.log(Level.INFO, "Initializing topology bean");
        consulClient = Consul.builder().withHttps(config.https()).withHostAndPort(HostAndPort.fromParts(config.host(), config.port())).build();
        serviceName = "mbyte.manager";
        instanceId = serviceName.concat(".1");
        Registration service = this.buildRegistration();
        consulClient.agentClient().register(service);
        try {
            consulClient.agentClient().pass(instanceId);
            registered = true;
            LOGGER.log(Level.INFO, "Instance registered with id=" + instanceId);
        } catch (NotRegisteredException e) {
            LOGGER.log(Level.WARNING, "Unable to checkin topology registration", e);
        }
    }

    @PreDestroy
    public void stop() {
        LOGGER.log(Level.INFO, "Stopping topology bean");
        if (!config.enabled()) {
            LOGGER.log(Level.INFO, "Topology service is disabled");
            return;
        }
        if (registered) {
            consulClient.agentClient().deregister(instanceId);
            this.registered = false;
            LOGGER.log(Level.INFO, "Service instance unregistered for id=" + instanceId);
        }
    }

    @Override
    public boolean isRegistered() {
        return registered;
    }

    @Override
    @Scheduled(every = "10s")
    public void checkin() {
        if (registered) {
            LOGGER.log(Level.FINEST, "Checkin manager in topology");
            try {
                consulClient.agentClient().pass(instanceId);
            } catch (NotRegisteredException e) {
                LOGGER.log(Level.WARNING, "Error while trying to checkin service in topology", e);
                Registration service = this.buildRegistration();
                consulClient.agentClient().deregister(instanceId);
                this.registered = false;
                consulClient.agentClient().register(service);
                this.registered = true;
            }
        }
    }

    @Override
    public String lookup(String id) {
        if (!config.enabled()) {
            LOGGER.log(Level.INFO, "Topology service is disabled");
            return null;
        }
        CatalogClient catalog = consulClient.catalogClient();
        List<CatalogService> services =  catalog.getService(id).getResponse();
        LOGGER.log(Level.INFO, "Services list: " + services);
        Optional<String> fqdn = services.stream().flatMap(s -> s.getServiceTags().stream().filter(tag -> tag.startsWith("fqdn"))).findFirst();
        return fqdn.orElse(null);
    }

    private Registration buildRegistration() {
        return ImmutableRegistration.builder()
                .id(instanceId)
                .name(serviceName)
                .check(ImmutableRegCheck.builder().ttl(String.format("%ss", 30l)).deregisterCriticalServiceAfter(String.format("%sh",1)).build())
                .build();
    }
}
