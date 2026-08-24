package com.vertyll.veds.template.infrastructure.config

import com.vertyll.veds.sharedtranslation.TranslationCatalogue
import com.vertyll.veds.sharedtranslation.translations
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal class TranslationCatalogueConfig {
    @Bean
    fun templateTranslationCatalogue(): TranslationCatalogue =
        translations("template-service") {
            key("template.not_found") {
                pl("Nie znaleziono zasobu.")
                en("Resource not found.")
            }
            key("template.access_denied") {
                pl("Brak uprawnień do tej operacji.")
                en("You do not have permission to do this.")
            }
            key("template.already_exists") {
                pl("Zasób już istnieje.")
                en("This resource already exists.")
            }
            key("template.misconfigured") {
                pl("Usługa jest nieprawidłowo skonfigurowana.")
                en("The service is misconfigured.")
            }
        }
}
