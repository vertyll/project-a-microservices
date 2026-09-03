rootProject.name = "iam-service"

include(":domain", ":application", ":infrastructure")

project(":domain").name = "iam-domain"
project(":application").name = "iam-application"
project(":infrastructure").name = "iam-infrastructure"

includeBuild("../shared-saga-api")
includeBuild("../shared-web")
includeBuild("../shared-archunit")
includeBuild("../shared-authz")
includeBuild("../shared-translation")
includeBuild("../shared-translation-client")
includeBuild("../shared-saga-engine")
includeBuild("../shared-messaging-kafka")
includeBuild("../iam-contracts")
includeBuild("../mail-contracts")
