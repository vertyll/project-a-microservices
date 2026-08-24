package com.vertyll.veds.template.application.port.inbound.command

import com.vertyll.veds.template.application.command.CreateTemplateCommand
import com.vertyll.veds.template.domain.model.Template

interface TemplateCommandUseCase {
    fun processTemplateWithSaga(command: CreateTemplateCommand): Template
}
