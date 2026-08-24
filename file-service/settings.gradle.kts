rootProject.name = "file-service"

include(":domain", ":application", ":infrastructure")

project(":domain").name = "file-domain"
project(":application").name = "file-application"
project(":infrastructure").name = "file-infrastructure"

includeBuild("../shared-contracts")
includeBuild("../shared-infrastructure")
includeBuild("../file-contracts")
