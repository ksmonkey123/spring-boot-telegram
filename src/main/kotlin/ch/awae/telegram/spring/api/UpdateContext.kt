package ch.awae.telegram.spring.api

import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.bots.AbsSender

open class UpdateContext<P: Principal> (val bot: AbsSender, val update: Update, val principal: P?)