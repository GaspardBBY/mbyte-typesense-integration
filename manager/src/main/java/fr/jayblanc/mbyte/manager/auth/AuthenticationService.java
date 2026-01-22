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
import fr.jayblanc.mbyte.manager.core.AccessDeniedException;

public interface AuthenticationService {

    String UNAUTHENTIFIED_IDENTIFIER = "anonymous";

    boolean isAuthentified();

    String getConnectedIdentifier();

    boolean isConnectedIdentifierInRoleAdmin();

    Profile getConnectedProfile();

    Profile getProfile(String id) throws AccessDeniedException, ProfileNotFoundException;

}
