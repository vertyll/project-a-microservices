rootProject.name = "mail-service"

include(":domain", ":application", ":infrastructure")

project(":domain").name = "mail-domain"
project(":application").name = "mail-application"
project(":infrastructure").name = "mail-infrastructure"

includeBuild("../shared-saga-api")
includeBuild("../shared-error")
includeBuild("../shared-web")
includeBuild("../shared-archunit")
includeBuild("../shared-translation")
includeBuild("../shared-translation-client")
includeBuild("../shared-saga-engine")
includeBuild("../shared-messaging-kafka")
includeBuild("../shared-authz")
includeBuild("../shared-authz-client")
includeBuild("../iam-contracts")
includeBuild("../mail-contracts")
