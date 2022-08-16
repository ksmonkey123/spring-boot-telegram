package ch.awae.telegram.spring.api

import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.bots.AbsSender

open class SimpleContext(
    override val bot: AbsSender,
    override val principal: Principal?,
    override val update: Update,
) : UpdateContext