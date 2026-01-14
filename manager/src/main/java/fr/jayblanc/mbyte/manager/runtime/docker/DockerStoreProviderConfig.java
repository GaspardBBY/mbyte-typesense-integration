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

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

import java.util.Optional;

/**
 * @author Jerome Blanchard
 */
@ConfigMapping(prefix = "manager.store.provider.docker")
public interface DockerStoreProviderConfig {

    /**
     * Docker server URL.
     * Examples:
     * - unix:///var/run/docker.sock (local)
     * - tcp://192.168.1.100:2375 (TCP unsecured)
     * - tcp://192.168.1.100:2376 (TCP with TLS)
     */
    String server();

    String image();

    String instanceName();

    /**
     * TLS configuration for secure Docker connections
     */
    @WithName("tls")
    Tls tls();

    interface Tls {

        /**
         * Enable TLS verification (default: false)
         */
        @WithName("verify")
        @WithDefault("false")
        boolean verify();

        /**
         * Path to the directory containing TLS certificates (ca.pem, cert.pem, key.pem)
         * Optional - only required if TLS verification is enabled
         */
        @WithName("cert.path")
        Optional<String> certPath();
    }
}
