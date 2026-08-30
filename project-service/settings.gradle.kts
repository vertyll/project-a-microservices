rootProject.name = "project-service"

include(":domain", ":application", ":infrastructure")

project(":domain").name = "project-domain"
project(":application").name = "project-application"
project(":infrastructure").name = "project-infrastructure"

includeBuild("../shared-saga-api")
includeBuild("../shared-web")
includeBuild("../shared-translation")
includeBuild("../shared-translation-client")
includeBuild("../shared-saga-engine")
includeBuild("../shared-messaging-kafka")
includeBuild("../project-contracts")
includeBuild("../mail-contracts")
includeBuild("../iam-contracts")
