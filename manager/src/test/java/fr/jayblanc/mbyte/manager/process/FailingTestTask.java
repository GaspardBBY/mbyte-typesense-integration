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
package fr.jayblanc.mbyte.manager.process;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.TransactionScoped;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Jerome Blanchard
 */
@TransactionScoped
public class FailingTestTask extends Task {

    private static final Logger LOGGER = Logger.getLogger(FailingTestTask.class.getName());
    public static final String NAME = "FailingTask";

    @PostConstruct
    public void init() {
        LOGGER.info("FailingTestTaskHandler initialized");
    }

    @Override
    public String getTaskName() {
        return NAME;
    }

    @Override
    public void execute() throws TaskException {
        LOGGER.info("FailingTestTaskHandler execution...");
        LOGGER.log(Level.INFO, "Executing " + getTaskName());
        this.setGlobalContextValue(FailingTestTask.NAME, "plop");
        this.fail("Failing task failed !!");
        throw new TaskException("Failing task failed (as expected) !!");
    }

    @Override
    public void rollback() throws TaskException {
        LOGGER.info("FailingTestTaskHandler rollback...");
        LOGGER.info("Cleaning context...");
        this.unsetGlobalContextValue(FailingTestTask.NAME);
        this.rollback("Rollback required");
        LOGGER.info("Context cleaned");
    }

}
