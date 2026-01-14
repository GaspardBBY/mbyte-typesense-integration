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
package fr.jayblanc.mbyte.manager.api.resources;

import fr.jayblanc.mbyte.manager.auth.AuthenticationService;
import fr.jayblanc.mbyte.manager.auth.entity.Profile;
import fr.jayblanc.mbyte.manager.core.CoreService;
import fr.jayblanc.mbyte.manager.core.CoreServiceException;
import fr.jayblanc.mbyte.manager.core.StoreNotFoundException;
import fr.jayblanc.mbyte.manager.core.entity.Store;
import fr.jayblanc.mbyte.manager.exception.AccessDeniedException;
import fr.jayblanc.mbyte.manager.process.ProcessEngine;
import fr.jayblanc.mbyte.manager.process.entity.Process;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("profiles")
public class ProfilesResource {

    private static final Logger LOGGER = Logger.getLogger(ProfilesResource.class.getName());

    @Inject AuthenticationService auth;
    @Inject CoreService core;
    @Inject ProcessEngine engine;
    @Inject Template profile;

    @GET
    @Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON})
    public Response profiles(@Context UriInfo uriInfo) {
        LOGGER.log(Level.INFO, "GET /api/profiles");
        String connectedId = auth.getConnectedIdentifier();
        URI root = uriInfo.getRequestUriBuilder().path(connectedId).build();
        return Response.seeOther(root).build();
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response profile(@PathParam("id") String id) {
        LOGGER.log(Level.INFO, "GET /api/profiles/" + id);
        Profile profile = auth.getConnectedProfile();
        return Response.ok(profile).build();
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance profileView(@PathParam("id") String id) {
        LOGGER.log(Level.INFO, "GET /api/profiles/" + id + " (html)");
        TemplateInstance view = profile.data("profile", auth.getConnectedProfile());
        try {
            String sid = core.listConnectedUserStores().getFirst();
            view = view.data("store", core.getStore(sid));
        } catch (NoSuchElementException | StoreNotFoundException | CoreServiceException | AccessDeniedException e ) {
            //
        }
        return view;
    }

    @GET
    @Path("{id}/stores")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> listProfileStores(@PathParam("id") String id) {
        LOGGER.log(Level.INFO, "GET /api/profiles/" + id + "/stores");
        return core.listConnectedUserStores();
    }

    @GET
    @Path("{id}/stores/{sid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Store getProfileStore(@PathParam("id") String id, @PathParam("sid") String sid) throws StoreNotFoundException, CoreServiceException, AccessDeniedException {
        LOGGER.log(Level.INFO, "GET /api/profiles/" + id + "/stores/" + sid);
        return core.getStore(sid);
    }

    @GET
    @Path("{id}/stores/{sid}/processes")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Process> getProfileStoreProcesses(@PathParam("id") String id, @PathParam("sid") String sid, @QueryParam("active") @DefaultValue("true") boolean active)
            throws StoreNotFoundException, CoreServiceException, AccessDeniedException {
        LOGGER.log(Level.INFO, "GET /api/profiles/" + id + "/stores/processes?active=" + active);
        Store store = core.getStore(sid);
        if (active) {
            return engine.findRunningProcessesForStore(store.getId());
        }
        return engine.findAllProcessesForStore(store.getId());
    }

    @POST
    @Path("{id}/stores")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createProfileStore(@PathParam("id") String id, MultivaluedMap<String, String> form, @Context UriInfo uriInfo) throws CoreServiceException {
        LOGGER.log(Level.INFO, "POST /api/profiles/" + id + "/stores");
        String storeId = core.createStore(form.getFirst("name"));
        LOGGER.log(Level.INFO, "Store created with id: " + storeId);
        URI location = uriInfo.getBaseUriBuilder().path(ProfilesResource.class).path(id).path("stores").path(storeId).build();
        return Response.created(location).entity(java.util.Map.of("storeId", storeId)).build();
    }


}
