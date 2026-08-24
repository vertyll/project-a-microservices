package com.vertyll.veds.task.infrastructure.config

import com.vertyll.veds.sharedtranslation.TranslationCatalogue
import com.vertyll.veds.sharedtranslation.translations
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal class TranslationCatalogueConfig {
    @Bean
    fun taskTranslationCatalogue(): TranslationCatalogue =
        translations("task-service") {
            key("task.not_found") {
                pl("Nie znaleziono zadania.")
                en("Task not found.")
            }
            key("task.archived") {
                pl("Zadanie jest zarchiwizowane.")
                en("This task is archived.")
            }
            key("task.access_denied") {
                pl("Nie masz uprawnień do tego zadania.")
                en("You do not have permission for this task.")
            }
            key("task.comment.not_found") {
                pl("Nie znaleziono komentarza.")
                en("Comment not found.")
            }
            key("task.comment.not_authored_by_caller") {
                pl("Możesz edytować tylko własne komentarze.")
                en("You can only edit your own comments.")
            }
            key("task.project.not_known") {
                pl("Projekt nie jest jeszcze znany tej usłudze.")
                en("This service does not know that project yet.")
            }
            key("task.project.archived") {
                pl("Projekt jest zarchiwizowany.")
                en("The project is archived.")
            }
            key("task.status.not_in_project") {
                pl("Ten status nie należy do projektu.")
                en("That status does not belong to the project.")
            }
            key("task.category.not_in_project") {
                pl("Ta kategoria nie należy do projektu.")
                en("That category does not belong to the project.")
            }
            key("task.assignee.not_a_member") {
                pl("Ta osoba nie jest członkiem projektu.")
                en("That person is not a member of the project.")
            }
            key("validation.task.description_required") {
                pl("Opis zadania jest wymagany.")
                en("Task description is required.")
            }
            key("validation.task.estimation_negative") {
                pl("Wycena nie może być ujemna.")
                en("The estimate cannot be negative.")
            }
            key("validation.task.worked_time_negative") {
                pl("Czas pracy nie może być ujemny.")
                en("Logged time cannot be negative.")
            }
            key("validation.task.batch_empty") {
                pl("Nie wybrano żadnego zadania.")
                en("No tasks were selected.")
            }
            key("validation.task.comment_content_required") {
                pl("Treść komentarza jest wymagana.")
                en("Comment content is required.")
            }
            key("task.created") {
                pl("Zadanie zostało utworzone.")
                en("Task created.")
            }
            key("task.updated") {
                pl("Zadanie zostało zaktualizowane.")
                en("Task updated.")
            }
            key("task.retrieved") {
                pl("Zadanie wczytane.")
                en("Task loaded.")
            }
            key("task.list_retrieved") {
                pl("Lista zadań wczytana.")
                en("Tasks loaded.")
            }
            key("task.batch_archived") {
                pl("Zarchiwizowano wybrane zadania.")
                en("Selected tasks archived.")
            }
            key("task.work_logged") {
                pl("Czas pracy zapisany.")
                en("Work logged.")
            }
            key("task.permissions_retrieved") {
                pl("Uprawnienia wczytane.")
                en("Permissions loaded.")
            }
            key("task.comment.added") {
                pl("Komentarz dodany.")
                en("Comment added.")
            }
            key("task.comment.updated") {
                pl("Komentarz zaktualizowany.")
                en("Comment updated.")
            }
            key("task.comment.deleted") {
                pl("Komentarz usunięty.")
                en("Comment deleted.")
            }
            key("task.comment.list_retrieved") {
                pl("Komentarze wczytane.")
                en("Comments loaded.")
            }
        }
}
