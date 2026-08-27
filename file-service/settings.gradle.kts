rootProject.name = "file-service"

include(":domain", ":application", ":infrastructure")

project(":domain").name = "file-domain"
project(":application").name = "file-application"
project(":infrastructure").name = "file-infrastructure"

includeBuild("../shared-web")
includeBuild("../shared-translation-client")
includeBuild("../shared-messaging-kafka")
includeBuild("../file-contracts")
