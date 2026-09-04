rootProject.name = "task-service"

include(":domain", ":application", ":infrastructure")

project(":domain").name = "task-domain"
project(":application").name = "task-application"
project(":infrastructure").name = "task-infrastructure"

includeBuild("../shared-web")
includeBuild("../shared-archunit")
includeBuild("../shared-error")
includeBuild("../shared-authz")
includeBuild("../shared-authz-client")
includeBuild("../shared-translation")
includeBuild("../shared-translation-client")
includeBuild("../shared-messaging-kafka")
includeBuild("../task-contracts")
includeBuild("../project-contracts")
includeBuild("../iam-contracts")
includeBuild("../file-contracts")
