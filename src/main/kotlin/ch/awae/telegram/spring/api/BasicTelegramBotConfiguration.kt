package ch.awae.telegram.spring.api

import ch.awae.telegram.spring.internal.AnonymousPrincipal
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.bots.AbsSender

abstract class BasicTelegramBotConfiguration : TelegramBotConfiguration<Principal, UpdateContext<Principal>>() {

    override fun resolvePrincipal(userId: Long): Principal? = AnonymousPrincipal(userId)

    override fun buildUpdateContext(bot: AbsSender, update: Update, principal: Principal?): UpdateContext<Principal> = SimpleContext(bot, update, principal)

}