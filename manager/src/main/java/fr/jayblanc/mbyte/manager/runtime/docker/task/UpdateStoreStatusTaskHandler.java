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
package fr.jayblanc.mbyte.manager.runtime.docker.task;

import fr.jayblanc.mbyte.manager.core.CoreServiceAdmin;
import fr.jayblanc.mbyte.manager.core.entity.Store;
import fr.jayblanc.mbyte.manager.process.TaskException;
import fr.jayblanc.mbyte.manager.process.TaskHandler;
import jakarta.inject.Inject;
import jakarta.transaction.TransactionScoped;

/**
 * @author Jerome Blanchard
 */
@TransactionScoped
public class UpdateStoreStatusTaskHandler extends TaskHandler {

    public static final String TASK_NAME = "UpdateStoreStatus";
    public static final String STORE_ID = "STORE_ID";
    public static final String STORE_STATUS = "STORE_STATUS";

    @Inject CoreServiceAdmin core;

    @Override public String getTaskName() {
        return TASK_NAME;
    }

    @Override public void execute() throws TaskException {
        String storeId = getMandatoryContextValue(STORE_ID);
        String storeStatus = getMandatoryContextValue(STORE_STATUS);

        try {
            core.systemUpdateStoreStatus(storeId, Store.Status.valueOf(storeStatus));
        } catch (Exception e) {
            this.fail(String.format("Failed to update store status for id: '%s' to status: '%s' - %s", storeId, storeStatus, e.getMessage()));
            throw new TaskException("Store status update failed for id: " + storeId + " to status: " + storeStatus, e);
        }
        this.complete(String.format("Store status updated for id: '%s' to status: '%s'", storeId, storeStatus));
    }
}

