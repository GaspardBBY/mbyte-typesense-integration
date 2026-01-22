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

import fr.jayblanc.mbyte.manager.core.entity.Application;
import fr.jayblanc.mbyte.manager.core.entity.Environment;
import fr.jayblanc.mbyte.manager.core.entity.EnvironmentEntry;
import fr.jayblanc.mbyte.manager.process.ProcessAlreadyRunningException;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CoreService {

    String createApp(String type, String name) throws ApplicationDescriptorNotFoundException;

    List<Application> listConnectedUserApps();

    List<Application> listApps(String owner) throws AccessDeniedException;

    Application getApp(String id) throws ApplicationNotFoundException, AccessDeniedException;

    Environment getAppEnv(String id) throws ApplicationNotFoundException, AccessDeniedException, EnvironmentNotFoundException;

    Environment updateAppEnv(String id, Set<EnvironmentEntry> entries) throws ApplicationNotFoundException, AccessDeniedException, EnvironmentNotFoundException;

    String runAppCommand(String id, String name, Map<String, String> params)
            throws ApplicationNotFoundException, AccessDeniedException, EnvironmentNotFoundException, ApplicationCommandNotFoundException,
            ProcessAlreadyRunningException;

    void dropApp(String id) throws ApplicationNotFoundException, CoreServiceException;

}
