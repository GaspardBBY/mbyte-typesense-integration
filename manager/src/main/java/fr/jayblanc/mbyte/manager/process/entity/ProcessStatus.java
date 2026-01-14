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
package fr.jayblanc.mbyte.manager.process.entity;

import java.util.List;

public enum ProcessStatus {

    CREATED,
    PENDING,
    TASK_ASSIGNED,
    TASK_RUNNING,
    SUSPENDED,
    COMPLETED,
    FAILED,
    ROLLED_BACK;

    public boolean isFinal() {
        return getCompletedProcessStatuses().contains(this);
    }

    public boolean isRunning() {
        return getRunningProcessStatuses().contains(this);
    }

    public static List<ProcessStatus> getRunningProcessStatuses() {
        return List.of(CREATED, PENDING, TASK_ASSIGNED, TASK_RUNNING, SUSPENDED);
    }

    public  static List<ProcessStatus> getCompletedProcessStatuses() {
        return List.of(COMPLETED, FAILED, ROLLED_BACK);
    }

}
