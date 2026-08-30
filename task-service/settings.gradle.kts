rootProject.name = "task-service"

include(":domain", ":application", ":infrastructure")

project(":domain").name = "task-domain"
project(":application").name = "task-application"
project(":infrastructure").name = "task-infrastructure"

includeBuild("../shared-saga-api")
includeBuild("../shared-web")
includeBuild("../shared-translation")
includeBuild("../shared-translation-client")
includeBuild("../shared-saga-engine")
includeBuild("../shared-messaging-kafka")
includeBuild("../task-contracts")
includeBuild("../project-contracts")
includeBuild("../iam-contracts")
includeBuild("../file-contracts")
