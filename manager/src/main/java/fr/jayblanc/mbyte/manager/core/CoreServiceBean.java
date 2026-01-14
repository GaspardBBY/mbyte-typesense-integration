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
import fr.jayblanc.mbyte.manager.core.entity.Store;
import fr.jayblanc.mbyte.manager.exception.AccessDeniedException;
import fr.jayblanc.mbyte.manager.process.ProcessAlreadyRunningException;
import fr.jayblanc.mbyte.manager.runtime.Runtime;
import fr.jayblanc.mbyte.manager.runtime.RuntimeProviderException;
import fr.jayblanc.mbyte.manager.runtime.RuntimeProviderNotFoundException;
import fr.jayblanc.mbyte.manager.topology.TopologyService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class CoreServiceBean implements CoreService, CoreServiceAdmin {

    private static final Logger LOGGER = Logger.getLogger(CoreServiceBean.class.getName());

    @Inject EntityManager em;
    @Inject AuthenticationService authenticationService;
    @Inject Runtime runtime;
    @Inject TopologyService topology;

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public String createStore(String name) {
        LOGGER.log(Level.INFO, "Creating new store with name: " + name);
        String id = UUID.randomUUID().toString();
        Store store = new Store();
        store.setId(id);
        store.setName(name);
        store.setCreationDate(System.currentTimeMillis());
        store.setOwner(authenticationService.getConnectedProfile().getUsername());
        store.setUsage(0);
        store.setStatus(Store.Status.CREATED);
        em.persist(store);
        try {
            String pid = runtime.getProvider().startStore(id, name, store.getOwner());
            LOGGER.log(Level.INFO, "Start Store Process pid: " + pid);
        } catch (RuntimeProviderException | RuntimeProviderNotFoundException | ProcessAlreadyRunningException e ) {
            LOGGER.log(Level.INFO, "Unable to start store, see logs", e);
        }
        return id;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public List<String> listConnectedUserStores() {
        LOGGER.log(Level.INFO, "Listing stores for connected user");
        String owner = authenticationService.getConnectedProfile().getUsername();
        return findStoresByOwner(owner);
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public Store getStore(String id) throws StoreNotFoundException, AccessDeniedException {
        LOGGER.log(Level.INFO, "Getting store for id: " + id);
        Store store = findStoreById(id);
        if ( !authenticationService.getConnectedProfile().getUsername().equals(store.getOwner()) ) {
            throw new AccessDeniedException("Access denied to store for id: " + id);
        }
        store.setLocation(locateStore(id));
        return store;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public Store systemGetStore(String id) throws StoreNotFoundException {
        LOGGER.log(Level.INFO, "Getting store for id: " + id);
        return findStoreById(id);
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void systemUpdateStoreStatus(String id, Store.Status status) throws StoreNotFoundException {
        LOGGER.log(Level.INFO, "## SYSTEM ## Updating store status for id: " + id + " to status: " + status);
        Store store = findStoreById(id);
        store.setStatus(status);
        em.merge(store);
    }

    private Store findStoreById(String id) throws StoreNotFoundException {
        Store store = em.find(Store.class, id);
        if ( store == null ) {
            throw new StoreNotFoundException("Unable to find a store for id: " + id);
        }
        return store;
    }

    private List<String> findStoresByOwner(String owner) {
        return em.createNamedQuery("Store.findIdByOwner", String.class).setParameter("owner", owner).getResultList();
    }

    private String locateStore(String id) {
        String location = topology.lookup(id);
        if ( location != null ) {
            LOGGER.log(Level.INFO, "Store instance located at: " + location);
            return location;
        }
        LOGGER.log(Level.INFO, "Unable to locate store in the topology");
        return "## unavailable ##";
    }

}
