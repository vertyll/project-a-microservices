rootProject.name = "iam-service"

include(":domain", ":application", ":infrastructure")

project(":domain").name = "iam-domain"
project(":application").name = "iam-application"
project(":infrastructure").name = "iam-infrastructure"

includeBuild("../shared-saga-api")
includeBuild("../shared-web")
includeBuild("../shared-saga-engine")
includeBuild("../shared-infrastructure")
includeBuild("../iam-contracts")
includeBuild("../mail-contracts")
