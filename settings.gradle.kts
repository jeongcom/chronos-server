pluginManagement { repositories { gradlePluginPortal(); mavenCentral() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { mavenCentral() } }
rootProject.name = "chronos-server"
include("chronos-contract", "chronos-domain", "chronos-application", "chronos-infrastructure", "chronos-api", "chronos-grpc", "chronos-boot", "chronos-device-gateway")
