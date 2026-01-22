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
package fr.jayblanc.mbyte.manager.core.runtime.dokku;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
@Startup
public class DokkuClientProducer {

    private static final Logger LOGGER = Logger.getLogger(DokkuClientProducer.class.getName());

    @Inject DokkuClientConfig config;

    @Produces
    @Singleton
    public DokkuClient createDokkuClient() {
        LOGGER.log(Level.FINE, "Creating dokku client");
        try {
            JSch.setConfig("StrictHostKeyChecking", "no");
            byte[] dokkuKey = IOUtils.resourceToByteArray("/ssh/manager_rsa");
            byte[] dokkuPubkey = IOUtils.resourceToByteArray("/ssh/manager_rsa.pub");
            byte[] knownHosts = IOUtils.resourceToByteArray("/ssh/known_hosts");
            JSch jsch = new JSch();
            jsch.addIdentity("dokku", dokkuKey, dokkuPubkey, null);
            jsch.setKnownHosts(new ByteArrayInputStream(knownHosts));
            return new DokkuClient(config.host(), config.port(), jsch);
        } catch (IOException | JSchException e) {
            LOGGER.log(Level.SEVERE, "unable to start DokkuStoreProvider", e);
            throw new RuntimeException("unable to start DokkuClient", e);
        }
    }



    /*
    @Override
    public List<String> listAllStores() throws RuntimeEngineException {
        LOGGER.log(Level.INFO, "[dokku] Listing store apps");
        String command = "apps:list";
        StringBuffer output = new StringBuffer();
        try {
            int status = execute("dokku", command, output);
            if ( status != 0 ) {
                throw new RuntimeEngineException("unable to list apps: " + output);
            }
            return Arrays.stream(output.toString().split("\\r?\\n")).filter(s -> !s.startsWith("=====>")).collect(Collectors.toList());
        } catch ( IOException | JSchException e ) {
            throw new RuntimeEngineException("unable to list apps", e);
        }
    }

    /*
    ssh dokku apps:create <pseudo>
    ssh dokku postgres:create <pseudo>-db
    ssh dokku postgres:link <appname>-db <pseudo>
    docker image save <appname>/filestore:24.2.1-SNAPSHOT | ssh dokku git:load-image <appname> <appname>/filestore:24.2.1-SNAPSHOT "Jayblanc" "jayblanc@gmail.com"
    ssh dokku storage:ensure-directory --chown heroku <appname>-data
    ssh dokku storage:mount <appname> /var/lib/dokku/data/storage/<appname>-data:/opt/jboss/filestore
    ssh dokku config:set <appname> \
          WILDFLY_ADMIN_PASSWORD=filestore \
          FILESTORE_OWNER=<appname> \
          FILESTORE_CONSUL_HOST=registry.miage22.jayblanc.fr \
          DB_USER=postgres \
          DB_PASS=93427d9d7ad9d97ef8bb4e299d1df5a8 \
          DB_HOST=dokku-postgres-<appname>-db \
          DB_PORT=5432 \
          DB_NAME=<appname>_db \
          OIDC_PROVIDER_URL=http://auth.miage23.jayblanc.fr/realms/Miage.23 \
          OIDC_CLIENT_ID=filestore \
          FILESTORE_HOME=/opt/jboss/filestore \
    ssh dokku ports:add <appname> http:80:8080
    * */

    /*
    @Override
    public String startStore(String id, java.util.Map<String, Object> config) throws RuntimeEngineException {
        String name = String.valueOf(config.get("name"));
        String owner = String.valueOf(config.get("owner"));

        LOGGER.log(Level.INFO, "[dokku] Creating new store apps");
        List<String> cmds = new ArrayList<>();
        cmds.add("apps:create " + id);
        cmds.add("postgres:create " + id + "-db --password password");
        cmds.add("postgres:link " + id + "-db " + id);
        cmds.add("storage:ensure-directory --chown heroku " + id + "-data");
        cmds.add("storage:mount " + id + " /var/lib/dokku/data/storage/" + id + "-data:/opt/jboss/filestore");
        cmds.add("config:set " + id + " WILDFLY_ADMIN_PASSWORD=filestore" +
                " OIDC_PROVIDER_URL=http://auth." + this.config.host() + "/realms/Miage.23" +
                " OIDC_CLIENT_ID=filestore" +
                " DB_USER=postgres" +
                " DB_PASS=password" +
                " DB_HOST=dokku-postgres-" + id + "-db" +
                " DB_PORT=5432" +
                " DB_NAME=" + id + "_db" +
                " FILESTORE_HOME=/opt/jboss/filestore" +
                " FILESTORE_CONSUL_HOST=registry." + this.config.host() +
                " FILESTORE_CONSUL_PORT=8500" +
                " FILESTORE_OWNER=" + owner +
                " FILESTORE_NAME=" + name +
                " FILESTORE_ID=" + id +
                " FILESTORE_FQDN=" + id + "." + this.config.host());
        cmds.add("ports:add " + id + " http:80:8080");
        cmds.add("git:from-image " + id + " " + this.config.image());

        try {
            StringBuffer output = new StringBuffer();
            for (String cmd : cmds) {
                LOGGER.log(Level.INFO, "[dokku] " + cmd);
                int status = execute("dokku", cmd, output);
                if (status != 0) {
                    throw new RuntimeEngineException("error during app creation: " + output);
                }
            }
            return output.toString();
        } catch ( IOException | JSchException e ) {
            throw new RuntimeEngineException("Unable to create app", e);
        }
    }

    @Override
    public String stopStore(String id) throws RuntimeEngineException {
        throw new RuntimeEngineException("Not implemented");
    }

    @Override
    public String destroyStore(String id) throws RuntimeEngineException {
        LOGGER.log(Level.INFO, "[dokku] Deleting app");
        String cmd1 = "--force apps:destroy " + id;

        try {
            StringBuffer output = new StringBuffer();
            LOGGER.log(Level.INFO, "[dokku] {}", cmd1);
            int status = execute("dokku", cmd1, output);
            if ( status != 0 ) {
                throw new RuntimeEngineException("unable to delete app: " + output);
            }
            LOGGER.log(Level.INFO, "[output] {}", output.toString());
        } catch ( IOException | JSchException e ) {
            throw new RuntimeEngineException("Unable to delete app", e);
        }

        return "";
    }

    */
}

