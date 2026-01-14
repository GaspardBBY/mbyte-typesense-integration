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
package fr.jayblanc.mbyte.manager.runtime;

import fr.jayblanc.mbyte.manager.process.ProcessAlreadyRunningException;

import java.util.List;

public interface RuntimeProvider {

    String name();

    List<String> listAllStores() throws RuntimeProviderException;

    String startStore(String id, String name, String owner) throws RuntimeProviderException, ProcessAlreadyRunningException;

    String stopStore(String id) throws RuntimeProviderException;

    String destroyStore(String id) throws RuntimeProviderException;

}
