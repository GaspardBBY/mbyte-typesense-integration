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
  logger?: unknown
  entries?: Record<string, Record<string, unknown>>
}

export type Process = {
  id: string
  owner?: string
  store?: string
  name?: string
  tasks?: string
  taskList?: string[]
  context?: ProcessContext
  status?: ProcessStatus
  nextTaskId?: number
  runningTaskJobId?: string
  creationDate?: number
  startDate?: number
  endDate?: number
}

