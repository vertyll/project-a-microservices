rootProject.name = "translation-service"

include(":domain", ":application", ":infrastructure")

project(":domain").name = "translation-domain"
project(":application").name = "translation-application"
project(":infrastructure").name = "translation-infrastructure"

includeBuild("../shared-web")
includeBuild("../shared-archunit")
includeBuild("../shared-translation")
includeBuild("../shared-authz")
includeBuild("../shared-authz-client")
includeBuild("../shared-messaging-kafka")
includeBuild("../iam-contracts")
