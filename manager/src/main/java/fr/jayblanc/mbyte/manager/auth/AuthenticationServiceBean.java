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
package fr.jayblanc.mbyte.manager.auth;

import fr.jayblanc.mbyte.manager.auth.entity.Profile;
import io.quarkus.oidc.UserInfo;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.logging.Level;
import java.util.logging.Logger;

@RequestScoped
public class AuthenticationServiceBean implements AuthenticationService {

    private static final Logger LOGGER = Logger.getLogger(AuthenticationServiceBean.class.getName());

    @Inject SecurityIdentity identity;
    @Inject AuthenticationConfig config;
    @Inject EntityManager em;
    @Inject UserInfo userInfo;

    @Override
    public boolean isAuthentified() {
        return !identity.isAnonymous();
    }

    @Override
    public String getConnectedIdentifier() {
        return isAuthentified()?identity.getPrincipal().getName():AuthenticationService.UNAUTHENTIFIED_IDENTIFIER;
    }

    @Override
    public boolean isConnectedIdentifierInRoleAdmin() {
        return identity.hasRole(config.adminRoleName());
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public Profile getConnectedProfile() {
        LOGGER.log(Level.FINE, "Getting connected profile");
        String connectedId = getConnectedIdentifier();
        LOGGER.log(Level.FINE, "Loading profile for connected identifier: {0}", connectedId);
        Profile profile = em.find(Profile.class, connectedId);
        if ( profile == null ) {
            Profile newprofile = new Profile();
            newprofile.setId(connectedId);
            if (userInfo != null)  {
                newprofile.setUsername(userInfo.getPreferredUserName());
                newprofile.setFullname(userInfo.getName() + " " + userInfo.getFamilyName());
                newprofile.setEmail(userInfo.getEmail());
            } else {
                LOGGER.log(Level.INFO, "Unable to access User Info");
            }
            em.persist(newprofile);
            LOGGER.log(Level.FINE, "Profile created for identifier: {0}", connectedId);
            profile = newprofile;
        } else if (userInfo != null && !profile.getEmail().equals(userInfo.getEmail())) {
            profile.setUsername(userInfo.getPreferredUserName());
            profile.setFullname(userInfo.getName() + " " + userInfo.getFamilyName());
            profile.setEmail(userInfo.getEmail());
            LOGGER.log(Level.FINE, "Profile email updated for identifier: {0}", connectedId);
        }
        LOGGER.log(Level.FINE, "Profile retrieved for connected identifier: {0}", profile);
        return profile;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public Profile getProfile(String id) throws ProfileNotFoundException {
        LOGGER.log(Level.FINE, "Getting profile for id: {0}", id);
        String connectedId = getConnectedIdentifier();
        if (!id.equals(connectedId) && !isConnectedIdentifierInRoleAdmin()) {
            throw new SecurityException("Access denied to profile: " + id);
        }
        LOGGER.log(Level.FINE, "Loading profile for id: {0}", id);
        Profile profile = em.find(Profile.class, id);
        if ( profile == null ) {
            throw new ProfileNotFoundException("Profile not found for id: " + id);
        }
        LOGGER.log(Level.FINE, "Profile retrieved: {0}", profile);
        return profile;
    }

}
