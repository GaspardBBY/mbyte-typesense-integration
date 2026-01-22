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
import fr.jayblanc.mbyte.manager.process.ProcessEngine;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("profiles")
public class ProfilesResource {

    private static final Logger LOGGER = Logger.getLogger(ProfilesResource.class.getName());

    @Inject AuthenticationService auth;
    @Inject CoreService core;
    @Inject ProcessEngine engine;

    @GET @Path("me") @Produces({ MediaType.TEXT_HTML, MediaType.APPLICATION_JSON }) public Response profiles(@Context UriInfo uriInfo) {
        LOGGER.log(Level.INFO, "GET /api/profiles/me");
        String connectedId = auth.getConnectedIdentifier();
        URI root = uriInfo.getBaseUriBuilder().path(ProfilesResource.class).path(connectedId).build();
        return Response.seeOther(root).build();
    }

    @GET @Path("{id}") @Produces(MediaType.APPLICATION_JSON) public Response profile(@PathParam("id") String id) {
        LOGGER.log(Level.INFO, "GET /api/profiles/{0}", id);
        Profile profile = auth.getConnectedProfile();
        return Response.ok(profile).build();
    }

    @GET @Path("{id}/apps") @Produces(MediaType.APPLICATION_JSON) public Response listApps(@PathParam("id") String id, @Context UriInfo uriInfo) {
        LOGGER.log(Level.INFO, "GET /api/profiles/{0}/apps", id);
        URI apps = uriInfo.getBaseUriBuilder().path(AppsResource.class).path(id).queryParam("owner", id).build();
        return Response.seeOther(apps).build();
    }
}
