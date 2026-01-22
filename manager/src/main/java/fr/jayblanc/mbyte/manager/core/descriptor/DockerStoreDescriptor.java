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
package fr.jayblanc.mbyte.manager.core.descriptor;

import fr.jayblanc.mbyte.manager.core.ApplicationDescriptor;
import fr.jayblanc.mbyte.manager.core.entity.Environment;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * @author Jerome Blanchard
 */
@ApplicationScoped
public class DockerStoreDescriptor implements ApplicationDescriptor {

    public static final String TYPE = "DOCKER_STORE";

    public enum EnvKey {
        STORE_NAME,
        STORE_OWNER,
        STORE_ENGINE,
        STORE_IMAGE_NAME,
        STORE_NETWORK_NAME,
        STORE_DB_VOLUME_NAME,
        STORE_DB_CONTAINER_NAME,
        STORE_DB_NAME,
        STORE_DB_USER,
        STORE_DB_PASSWORD,
        STORE_VOLUME_NAME,
        STORE_CONTAINER_NAME,
        STORE_FQDN,
        STORE_TOPOLOGY_ENABLED;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public Environment getInitialEnv(String realm, String id, String name, String owner) {
        Environment env = new Environment();
        env.setApp(id);
        env.addEntry(ApplicationDescriptor.EnvKey.APP_ID.name(), id);
        env.addEntry(EnvKey.STORE_NAME.name(), name);
        env.addEntry(EnvKey.STORE_OWNER.name(), owner);
        env.addEntry(EnvKey.STORE_ENGINE.name(), "docker");
        env.addEntry(EnvKey.STORE_IMAGE_NAME.name(), "jerome/store:25.1-SNAPSHOT");
        env.addEntry(EnvKey.STORE_NETWORK_NAME.name(), realm);
        env.addEntry(EnvKey.STORE_DB_VOLUME_NAME.name(), realm + "." + name + ".db");
        env.addEntry(EnvKey.STORE_DB_CONTAINER_NAME.name(), realm + "." + name + ".db");
        env.addEntry(EnvKey.STORE_DB_NAME.name(), "storedb");
        env.addEntry(EnvKey.STORE_DB_USER.name(), "storeuser");
        env.addSecretEntry(EnvKey.STORE_DB_PASSWORD.name(), "Pp@asSw#W0orRdD!");
        env.addEntry(EnvKey.STORE_VOLUME_NAME.name(), realm + "." + name + ".store");
        env.addEntry(EnvKey.STORE_CONTAINER_NAME.name(), realm + "." + name + ".store");
        env.addEntry(EnvKey.STORE_FQDN.name(), name + ".s." + realm);
        env.addEntry(EnvKey.STORE_TOPOLOGY_ENABLED.name(), true);
        return env;
    }

}
