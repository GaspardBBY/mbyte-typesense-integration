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
public class ApplicationCommandProvider {

    private static final  Logger LOGGER = Logger.getLogger(ApplicationCommandProvider.class.getName());

    @Inject Instance<ApplicationCommand> commands;

    public List<String> listAllCommands() {
        LOGGER.log(Level.FINE, "List available application commands");
        return commands.stream().map(ApplicationCommand::getName).toList();
    }

    public List<String> listCommandsForAppType(String type) {
        LOGGER.log(Level.FINE, "List available application commands for type: " + type);
        return commands.stream().filter(c -> c.forAppType().equals(type)).map(ApplicationCommand::getName).toList();
    }

    public ApplicationCommand findCommand(String type, String name) throws ApplicationCommandNotFoundException {
        LOGGER.log(Level.FINE, "Searching application command for name: " + name);
        return commands.stream()
                .filter(c -> c.forAppType().equals(type) && c.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new ApplicationCommandNotFoundException("unable to find a command with name: " + name + " for application type: " + type));
    }

}
