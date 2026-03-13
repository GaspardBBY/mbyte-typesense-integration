import type { Application } from '../api/entities/Application'

function statusRank(status: Application['status']) {
  switch (status) {
    case 'AVAILABLE':
      return 5
    case 'STARTED':
      return 4
    case 'CREATED':
      return 3
    case 'STOPPED':
      return 2
    case 'LOST':
    case 'ERROR':
      return 1
    default:
      return 0
  }
}

export function selectPreferredStoreApp(apps: Application[]) {
  const storeApps = apps.filter((app) => app.type === 'DOCKER_STORE')
  return storeApps.sort((left, right) => {
    const byStatus = statusRank(right.status) - statusRank(left.status)
    if (byStatus !== 0) {
      return byStatus
    }
    return (right.creationDate ?? 0) - (left.creationDate ?? 0)
  })[0]
}
