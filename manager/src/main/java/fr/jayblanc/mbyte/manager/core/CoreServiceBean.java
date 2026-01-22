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
package fr.jayblanc.mbyte.manager.core;

import fr.jayblanc.mbyte.manager.auth.AuthenticationService;
import fr.jayblanc.mbyte.manager.core.entity.Application;
import fr.jayblanc.mbyte.manager.core.entity.Environment;
import fr.jayblanc.mbyte.manager.core.entity.EnvironmentEntry;
import fr.jayblanc.mbyte.manager.process.ProcessAlreadyRunningException;
import fr.jayblanc.mbyte.manager.process.ProcessDefinition;
import fr.jayblanc.mbyte.manager.process.ProcessEngine;
import fr.jayblanc.mbyte.manager.topology.TopologyService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class CoreServiceBean implements CoreService, CoreServiceAdmin {

    private static final Logger LOGGER = Logger.getLogger(CoreServiceBean.class.getName());

    @Inject AuthenticationService authentication;
    @Inject ApplicationDescriptorRegistry appRegistry;
    @Inject ApplicationCommandProvider commandsProvider;
    @Inject ProcessEngine processEngine;
    @Inject TopologyService topology;
    @Inject CoreConfig config;
    @Inject EntityManager em;

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public String createApp(String type, String name) throws ApplicationDescriptorNotFoundException {
        LOGGER.log(Level.INFO, "Creating new application of type: {0} with name: {1}", new Object[] {type, name});
        String appid = UUID.randomUUID().toString();
        Application application = new Application();
        application.setId(appid);
        application.setName(name);
        application.setType(type);
        application.setCreationDate(System.currentTimeMillis());
        application.setOwner(authentication.getConnectedIdentifier());
        application.setStatus(ApplicationStatus.CREATED);
        em.persist(application);
        Environment initialEnv = appRegistry.findDescriptor(type).getInitialEnv(config.instance(), appid, name, application.getOwner());
        em.persist(initialEnv);
        return appid;
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public Environment getAppEnv(String id) throws ApplicationNotFoundException, AccessDeniedException, EnvironmentNotFoundException {
        LOGGER.log(Level.INFO, "Getting environment for app id: {0}", id);
        Application app = this.getApp(id);
        return this.findEnvByApp(app.getId());
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public Environment updateAppEnv(String id, Set<EnvironmentEntry> entries) throws ApplicationNotFoundException, AccessDeniedException, EnvironmentNotFoundException {
        LOGGER.log(Level.INFO, "Updating environment for app id: {0}", id);
        Application app = this.getApp(id);
        Environment env = this.findEnvByApp(app.getId());
        env.addAll(entries);
        return env;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public String runAppCommand(String id, String name, Map<String, String> params)
            throws ApplicationNotFoundException, AccessDeniedException, EnvironmentNotFoundException, ApplicationCommandNotFoundException,
            ProcessAlreadyRunningException {
        LOGGER.log(Level.INFO, "Running command: {0} for app id: {1}", new  Object[]{name, id});
        Application app = this.getApp(id);
        Environment env = this.findEnvByApp(app.getId());
        ApplicationCommand command = commandsProvider.findCommand(app.getType(), name);
        ProcessDefinition definition = command.buildProcessDefinition(env);
        String pid = processEngine.startProcess(definition);
        LOGGER.log(Level.INFO, "Started command's process: {0}",  pid);
        return pid;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public List<Application> listConnectedUserApps() {
        LOGGER.log(Level.INFO, "Listing applications for connected user");
        String owner = authentication.getConnectedIdentifier();
        return findAppsByOwner(owner);
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public List<Application> listApps(String owner) {
        LOGGER.log(Level.INFO, "Listing all applications");
        String connected = authentication.getConnectedIdentifier();
        if (owner == null || owner.isEmpty() || !connected.equals(owner)) {
            if (authentication.isConnectedIdentifierInRoleAdmin()) {
                return em.createNamedQuery("Application.findAll", Application.class).getResultList();
            } else {
                throw new SecurityException("Access denied to list applications for " + ((owner == null || owner.isEmpty())?"all users":"owner: " + owner));
            }
        }
        return findAppsByOwner(connected);
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public Application getApp(String id) throws ApplicationNotFoundException, AccessDeniedException {
        LOGGER.log(Level.INFO, "Getting application for id: {0}", id);
        Application application = findAppById(id);
        if ( !authentication.getConnectedIdentifier().equals(application.getOwner()) ) {
            throw new AccessDeniedException("Access denied to application with id: " + id);
        }
        return application;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public void dropApp(String id) throws ApplicationNotFoundException {
        LOGGER.log(Level.INFO, "Dropping application for id: {0}", id);
        Application application  = findAppById(id);
        em.remove(application);
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public Application systemGetApp(String id) throws ApplicationNotFoundException {
        LOGGER.log(Level.INFO, "Getting application for id: {0}", id);
        return findAppById(id);
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void systemUpdateAppStatus(String id, ApplicationStatus status) throws ApplicationNotFoundException {
        LOGGER.log(Level.INFO, "## SYSTEM ## Updating application status for id: {0} to status: {1}", new Object[]{id, status});
        Application application = findAppById(id);
        application.setStatus(status);
    }

    private Environment findEnvByApp(String appId) throws EnvironmentNotFoundException {
        List<Environment> envs = em.createNamedQuery("Environment.findByApp", Environment.class).setParameter("app", appId).getResultList();
        if ( envs == null || envs.isEmpty() ) {
            throw new EnvironmentNotFoundException("Unable to find an environment for app with id: " + appId);
        }
        if ( envs.size() > 1 ) {
            LOGGER.log(Level.WARNING, "Multiple environments found for app with id: {}, returning the first one", appId);
        }
        return envs.getFirst();
    }

    private Application findAppById(String id) throws ApplicationNotFoundException {
        Application application = em.find(Application.class, id);
        if ( application == null ) {
            throw new ApplicationNotFoundException("Unable to find an application for id: " + id);
        }
        return application;
    }

    private List<Application> findAppsByOwner(String owner) {
        return em.createNamedQuery("Application.findByOwner", Application.class).setParameter("owner", owner).getResultList();
    }

    private String locateApp(String id) {
        String location = topology.lookup(id);
        if ( location != null ) {
            LOGGER.log(Level.INFO, "Application instance located at: {0}", location);
            return location;
        }
        LOGGER.log(Level.INFO, "Unable to locate application in the topology");
        return "## unavailable ##";
    }

}
