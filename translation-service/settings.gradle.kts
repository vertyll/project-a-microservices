rootProject.name = "translation-service"

include(":domain", ":application", ":infrastructure")

project(":domain").name = "translation-domain"
project(":application").name = "translation-application"
project(":infrastructure").name = "translation-infrastructure"

includeBuild("../shared-web")
// ICU rendering and the key-declaration DSL.
includeBuild("../shared-translation")
