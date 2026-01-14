export type Store = {
  id: string
  type?: string
  owner?: string
  name?: string
  creationDate?: number
  usage?: number
  status?: 'CREATED' | 'STARTING' | 'AVAILABLE' | 'LOST' | 'ERROR'
  log?: string
  location?: string
}

