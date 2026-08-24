package com.vertyll.veds.mail.infrastructure.config

import com.vertyll.veds.sharedtranslation.TranslationCatalogue
import com.vertyll.veds.sharedtranslation.translations
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal class TranslationCatalogueConfig {
    @Bean
    fun mailTranslationCatalogue(): TranslationCatalogue =
        translations("mail-service") {
            key("mail.template.not_found") {
                pl("Nie znaleziono szablonu wiadomości.")
                en("Mail template not found.")
            }
            key("mail.template.render_failed") {
                pl("Nie udało się wygenerować wiadomości.")
                en("The message could not be rendered.")
            }
            key("mail.delivery.failed") {
                pl("Nie udało się wysłać wiadomości.")
                en("The message could not be delivered.")
            }
            key("mail.log.not_found") {
                pl("Nie znaleziono wpisu w dzienniku wysyłki.")
                en("Mail log entry not found.")
            }
            key("mail.batch.empty") {
                pl("Lista odbiorców jest pusta.")
                en("The recipient list is empty.")
            }
            key("validation.mail.recipients_required") {
                pl("Wymagany jest co najmniej jeden odbiorca.")
                en("At least one recipient is required.")
            }
            key("validation.mail.recipient_email_required") {
                pl("Adres odbiorcy jest wymagany.")
                en("The recipient address is required.")
            }
            key("validation.mail.subject_required") {
                pl("Temat jest wymagany.")
                en("A subject is required.")
            }
            key("validation.mail.template_name_required") {
                pl("Nazwa szablonu jest wymagana.")
                en("A template name is required.")
            }
        }
}
