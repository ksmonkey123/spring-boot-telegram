package ch.awae.telegram.spring.api

import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.bots.AbsSender

interface UpdateContext {
    val bot: AbsSender
    val principal: Principal?
    val update: Update
}