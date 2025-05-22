package com.vertyll.projecta.common.mail

/**
 * Constants for email template names used across the application.
 */
enum class EmailTemplate(val templateName: String) {
    // User registration and account management
    ACTIVATE_ACCOUNT("ACTIVATE_ACCOUNT"),
    WELCOME_EMAIL("WELCOME_EMAIL"),
    
    // Password management
    RESET_PASSWORD("RESET_PASSWORD"),
    CHANGE_PASSWORD("CHANGE_PASSWORD"),
    
    // Email management
    CHANGE_EMAIL("CHANGE_EMAIL");
    
    companion object {
        /**
         * Finds a template by its name.
         * @param name The template name to search for
         * @return The corresponding EmailTemplate or null if not found
         */
        fun fromTemplateName(name: String): EmailTemplate? = 
            EmailTemplate.entries.find { it.templateName == name }
    }
} 