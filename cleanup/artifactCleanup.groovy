//curl command example for running this plugin. 
//curl -i -uadmin:password -X POST "http://localhost:8081/artifactory/api/plugins/execute/cleanup?params=days=7|repos=libs-release-local|log|dryRun"
executions {
    cleanup() { params ->
        def days = params['days'] ? params['days'][0] as int: 6
        def repos = params['repos'] as String[]
        def dryRun = params['dryRun'] ? params['dryRun'][0] as boolean: false
        artifactCleanup(days, repos, log, dryRun)
    }
}

jobs {
    scheduledCleanup(cron: "0 0 5 ? * 1") {
        def config = new ConfigSlurper().parse(new File("${System.properties.'artifactory.home'}/etc/plugins/artifactCleanup.properties").toURL())
        artifactCleanup(config.daysUntil, config.repos as String[], log);
    }
}

private def artifactCleanup(int days, String[] repos, log, dryRun) {
    log.warn "Starting artifact cleanup for repositories $repos, until $days days ago"

    def daysUntil = Calendar.getInstance()
    daysUntil.add(Calendar.DAY_OF_MONTH, -days)

    def artifactsCleanedUp =
        searches.artifactsNotDownloadedSince(daysUntil, daysUntil, repos).
        each {
            if (dryRun) {
                log.warn "Found $it";
            } else {
                log.warn "Deleting $it";
                repositories.delete it
            }
        }

    if (dryRun) {
        log.warn "Dry run - nothing deleted. found $artifactsCleanedUp.size artifacts"
    } else {
        log.warn "Finished cleanup, deleted $artifactsCleanedUp.size artifacts"
    }
}
