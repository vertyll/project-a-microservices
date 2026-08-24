rootProject.name = "project-service"

include(":domain", ":application", ":infrastructure")

project(":domain").name = "project-domain"
project(":application").name = "project-application"
project(":infrastructure").name = "project-infrastructure"

includeBuild("../shared-contracts")
includeBuild("../shared-infrastructure")
includeBuild("../project-contracts")
includeBuild("../mail-contracts")
includeBuild("../iam-contracts")
