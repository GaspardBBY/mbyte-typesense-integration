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
import fr.jayblanc.mbyte.manager.notification.NotificationService;
import fr.jayblanc.mbyte.manager.notification.NotificationServiceException;
import fr.jayblanc.mbyte.manager.notification.entity.Event;
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

import static org.bouncycastle.oer.its.ieee1609dot2.EndEntityType.app;

@ApplicationScoped
public class CoreServiceBean implements CoreService, CoreServiceAdmin {

    private static final Logger LOGGER = Logger.getLogger(CoreServiceBean.class.getName());

    @Inject AuthenticationService authentication;
    @Inject NotificationService notification;
    @Inject ApplicationDescriptorRegistry appRegistry;
    @Inject ApplicationCommandProvider commandsProvider;
    @Inject ProcessEngine processEngine;
    @Inject TopologyService topology;
    @Inject CoreConfig config;
    @Inject EntityManager em;

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public String createApp(String type, String name) throws ApplicationDescriptorNotFoundException, NotificationServiceException {
        LOGGER.log(Level.INFO, "Creating new application of type: {0} with name: {1}", new Object[] {type, name});
        String owner = authentication.getConnectedIdentifier();
        if ("DOCKER_STORE".equals(type)) {
            List<Application> existingApps = em.createNamedQuery("Application.findByOwnerAndType", Application.class)
                    .setParameter("owner", owner)
                    .setParameter("type", type)
                    .getResultList();
            if (!existingApps.isEmpty()) {
                Application existing = existingApps.stream()
                        .sorted((left, right) -> {
                            int statusCompare = Integer.compare(storeStatusRank(right.getStatus()), storeStatusRank(left.getStatus()));
                            if (statusCompare != 0) {
                                return statusCompare;
                            }
                            return Long.compare(right.getCreationDate(), left.getCreationDate());
                        })
                        .findFirst()
                        .orElse(existingApps.getFirst());
                LOGGER.log(Level.INFO, "Reusing existing store application for owner: {0}, app id: {1}", new Object[] {owner, existing.getId()});
                return existing.getId();
            }
        }
        String appid = UUID.randomUUID().toString();
        Application application = new Application();
        application.setId(appid);
        application.setName(name);
        application.setType(type);
        application.setCreationDate(System.currentTimeMillis());
        application.setOwner(owner);
        application.setStatus(ApplicationStatus.CREATED);
        em.persist(application);
        Environment initialEnv = appRegistry.findDescriptor(type).getInitialEnv(config.instance(), appid, name, application.getOwner());
        em.persist(initialEnv);
        notification.notify(application.getOwner(), Event.TYPE_APP_CREATED, application.getId(),
                "Application created with id: " + application.getId(),
                Map.of("appId", application.getId(), "appName", application.getName(), "appType", application.getType()));
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
    public Environment updateAppEnv(String id, Set<EnvironmentEntry> entries)
            throws ApplicationNotFoundException, AccessDeniedException, EnvironmentNotFoundException, NotificationServiceException {
        LOGGER.log(Level.INFO, "Updating environment for app id: {0}", id);
        Application app = this.getApp(id);
        Environment env = this.findEnvByApp(app.getId());
        env.addAll(entries);
        notification.notify(app.getOwner(), Event.TYPE_ENV_UPDATED, id,
                "Environment updated for app id: " + id,
                Map.of("appId", app.getId(), "appName", app.getName()));
        return env;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public String runAppCommand(String id, String name, Map<String, String> params)
            throws ApplicationNotFoundException, AccessDeniedException, EnvironmentNotFoundException, ApplicationCommandNotFoundException,
            ProcessAlreadyRunningException, NotificationServiceException {
        LOGGER.log(Level.INFO, "Running command: {0} for app id: {1}", new  Object[]{name, id});
        Application app = this.getApp(id);
        Environment env = this.findEnvByApp(app.getId());
        ApplicationCommand command = commandsProvider.findCommand(app.getType(), name);
        ProcessDefinition definition = command.buildProcessDefinition(env);
        String pid = processEngine.startProcess(definition);
        LOGGER.log(Level.INFO, "Started command''s process: {0}",  pid);
        notification.notify(app.getOwner(), Event.TYPE_APP_COMMAND_RUN, app.getId(),
                "Running command: " + name + " for app id: " + id,
                Map.of("appId", app.getId(), "appName", app.getName(), "commandName", name, "processId", pid));
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
    public void dropApp(String id) throws ApplicationNotFoundException, NotificationServiceException {
        LOGGER.log(Level.INFO, "Dropping application for id: {0}", id);
        Application application  = findAppById(id);
        em.remove(application);
        notification.notify(application.getOwner(), Event.TYPE_APP_DELETED, application.getId(),
                "Application deleted with id: " + application.getId(),
                Map.of("appId", application.getId(), "appName", application.getName(), "appType", application.getType()));
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public Application systemGetApp(String id) throws ApplicationNotFoundException {
        LOGGER.log(Level.INFO, "Getting application for id: {0}", id);
        return findAppById(id);
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void systemUpdateAppStatus(String id, ApplicationStatus status) throws ApplicationNotFoundException,
            NotificationServiceException {
        LOGGER.log(Level.INFO, "## SYSTEM ## Updating application status for id: {0} to status: {1}", new Object[]{id, status});
        Application application = findAppById(id);
        application.setStatus(status);
        notification.notify(application.getOwner(), Event.TYPE_APP_STATUS_UPDATED, application.getId(),
                "Application status updated to: " + status,
                Map.of("appId", application.getId(), "appName", application.getName(), "appType", application.getType(), "appStatus", status.toString()));
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

    private int storeStatusRank(ApplicationStatus status) {
        if (status == null) {
            return 0;
        }
        return switch (status) {
            case AVAILABLE -> 5;
            case STARTED -> 4;
            case CREATED -> 3;
            case STOPPED -> 2;
            case LOST, ERROR -> 1;
        };
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
