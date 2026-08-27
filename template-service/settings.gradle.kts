rootProject.name = "template-service"

include(":domain", ":application", ":infrastructure")

project(":domain").name = "template-domain"
project(":application").name = "template-application"
project(":infrastructure").name = "template-infrastructure"

includeBuild("../shared-saga-api")
includeBuild("../shared-web")
includeBuild("../shared-translation-client")
includeBuild("../shared-saga-engine")
includeBuild("../shared-messaging-kafka")
includeBuild("../template-contracts")
