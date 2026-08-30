rootProject.name = "notification-service"

include(":domain", ":application", ":infrastructure")

project(":domain").name = "notification-domain"
project(":application").name = "notification-application"
project(":infrastructure").name = "notification-infrastructure"

includeBuild("../shared-saga-api")
includeBuild("../shared-web")
includeBuild("../shared-translation")
includeBuild("../shared-translation-client")
includeBuild("../shared-saga-engine")
includeBuild("../shared-messaging-kafka")
includeBuild("../notification-contracts")
includeBuild("../project-contracts")
includeBuild("../task-contracts")
includeBuild("../mail-contracts")
includeBuild("../iam-contracts")
