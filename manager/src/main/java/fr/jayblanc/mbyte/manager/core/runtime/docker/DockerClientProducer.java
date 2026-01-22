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
package fr.jayblanc.mbyte.manager.core.runtime.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Jerome Blanchard
 */
@ApplicationScoped
public class DockerClientProducer {

    private static final Logger LOGGER = Logger.getLogger(DockerClientProducer.class.getName());

    @Inject DockerClientConfig config;

    @Produces
    @Singleton
    public DockerClient createDockerClient() {
        LOGGER.log(Level.FINE, "Creating docker client");
        DefaultDockerClientConfig.Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder().withDockerHost(config.server());
        if (config.tls().verify()) {
            configBuilder.withDockerTlsVerify(true);
            config.tls().certPath().ifPresent(certPath -> {
                configBuilder.withDockerCertPath(certPath);
                LOGGER.log(Level.INFO, "TLS enabled with certificates from: " + certPath);
            });
            if (config.tls().certPath().isEmpty()) {
                LOGGER.log(Level.WARNING, "TLS verification enabled but no certificate path provided!");
            }
        }

        com.github.dockerjava.core.DockerClientConfig clientConfig = configBuilder.build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(clientConfig.getDockerHost())
                .sslConfig(clientConfig.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();

        LOGGER.log(Level.INFO, "DockerClient created with host: " + config.server() + " (TLS: " + config.tls().verify() + ")");
        return DockerClientImpl.getInstance(clientConfig, httpClient);
    }
}
