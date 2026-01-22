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

import io.quarkus.runtime.Startup;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
public class ApplicationDescriptorRegistry {

    private static final  Logger LOGGER = Logger.getLogger(ApplicationDescriptorRegistry.class.getName());

    @Inject Instance<ApplicationDescriptor> descriptors;

    public List<String> listDescriptors() {
        LOGGER.log(Level.FINE, "List available application descriptors");
        return descriptors.stream().map(ApplicationDescriptor::getType).toList();
    }

    public ApplicationDescriptor findDescriptor(String type) throws ApplicationDescriptorNotFoundException {
        LOGGER.log(Level.FINE, "Searching application descriptor for type: " + type);
        return descriptors.stream()
                .filter(c -> c.getType().equals(type)).findFirst()
                .orElseThrow(() -> new ApplicationDescriptorNotFoundException("unable to find a descriptor for app type: " + type));
    }

}
