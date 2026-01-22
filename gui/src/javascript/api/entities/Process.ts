///
/// Copyright (C) 2025 Jerome Blanchard <jayblanc@gmail.com>
///
/// This program is free software: you can redistribute it and/or modify
/// it under the terms of the GNU General Public License as published by
/// the Free Software Foundation, either version 3 of the License, or
/// (at your option) any later version.
///
/// This program is distributed in the hope that it will be useful,
/// but WITHOUT ANY WARRANTY; without even the implied warranty of
/// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
/// GNU General Public License for more details.
///
/// You should have received a copy of the GNU General Public License
/// along with this program.  If not, see <https://www.gnu.org/licenses/>.
///

export type ProcessStatus =
  | 'CREATED'
  | 'PENDING'
  | 'TASK_ASSIGNED'
  | 'TASK_RUNNING'
  | 'SUSPENDED'
  | 'COMPLETED'
  | 'FAILED'
  | 'ROLLED_BACK'

export type ProcessContext = {
  entries?: Record<string, Record<string, unknown>>
}

export type Process = {
  id: string
  owner?: string
  appId?: string
  name?: string
  taskList?: string[]
  context?: ProcessContext
  log?: string
  status?: ProcessStatus
  nextTaskId?: string
  runningTaskJobId?: string
  creationDate?: number
  startDate?: number
  endDate?: number
}

