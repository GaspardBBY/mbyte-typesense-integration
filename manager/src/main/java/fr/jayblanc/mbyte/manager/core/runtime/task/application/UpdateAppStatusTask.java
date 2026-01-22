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
package fr.jayblanc.mbyte.manager.core.runtime.task.application;

import fr.jayblanc.mbyte.manager.core.ApplicationStatus;
import fr.jayblanc.mbyte.manager.core.CoreServiceAdmin;
import fr.jayblanc.mbyte.manager.process.Task;
import fr.jayblanc.mbyte.manager.process.TaskException;
import jakarta.inject.Inject;
import jakarta.transaction.TransactionScoped;

/**
 * @author Jerome Blanchard
 */
@TransactionScoped
public class UpdateAppStatusTask extends Task {

    public static final String TASK_NAME = "UpdateAppStatus";
    public static final String APP_ID = "APP_ID";
    public static final String APP_STATUS = "APP_STATUS";

    @Inject CoreServiceAdmin core;

    @Override public String getTaskName() {
        return TASK_NAME;
    }

    @Override public void execute() throws TaskException {
        String storeId = getMandatoryContextValue(APP_ID);
        String storeStatus = getMandatoryContextValue(APP_STATUS);

        try {
            core.systemUpdateAppStatus(storeId, ApplicationStatus.valueOf(storeStatus));
        } catch (Exception e) {
            this.fail(String.format("Failed to update application status for id: '%s' to status: '%s' - %s", storeId, storeStatus, e.getMessage()));
            throw new TaskException("Application status update failed for id: " + storeId + " to status: " + storeStatus, e);
        }
        this.complete(String.format("Application status updated for id: '%s' to status: '%s'", storeId, storeStatus));
    }
}

