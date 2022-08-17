package ch.awae.telegram.spring.api

import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.bots.AbsSender

open class SimpleContext(bot : AbsSender, update: Update, principal: Principal?) : UpdateContext<Principal>(bot, update, principal)