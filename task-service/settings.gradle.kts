rootProject.name = "task-service"

include(":domain", ":application", ":infrastructure")

project(":domain").name = "task-domain"
project(":application").name = "task-application"
project(":infrastructure").name = "task-infrastructure"

includeBuild("../shared-contracts")
includeBuild("../shared-infrastructure")
includeBuild("../task-contracts")
includeBuild("../project-contracts")
includeBuild("../iam-contracts")
includeBuild("../file-contracts")
