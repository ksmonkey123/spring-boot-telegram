package ch.awae.telegram.spring.api

import ch.awae.telegram.spring.internal.AnonymousPrincipal
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.bots.AbsSender
import org.telegram.telegrambots.meta.generics.LongPollingBot
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

@ComponentScan("ch.awae.telegram.spring")
abstract class TelegramBotConfiguration {

    @Bean
    open fun getApi() = TelegramBotsApi(DefaultBotSession::class.java)

    open fun resolvePrincipal(userId: Long) : Principal = AnonymousPrincipal(userId)

    /** called whenever an authorization error occurs - i.e. no authorized handler was found for an update */
    open fun onUnauthorizedAccess(update: Update, principal: Principal?, bot: AbsSender) {}

    /**
     * called when a new update is being processed. this is called at the very beginning, before a handler has been
     * determined and before any authorization has been performed. The user principal has already been established
     * at this point, so it is provided as well.
     *
     * This function also serves as a filter for the further execution: if 'false' is returned or an exception is
     * thrown, the processing of the update is terminated.
     */
    open fun onUpdate(update: Update, principal: Principal?, bot: AbsSender) : Boolean = true

    /**
     * called when the handler for a message has been determined and authorization has been verified.
     * The call to this function is performed before the chosen handler is called.
     *
     * This function also serves as a filter for the handler execution: if 'false' is returned or an exception is
     * thrown, the processing of this update is terminated and no handlers are called.
     */
    open fun onAuthorizedAccess(update: Update, principal: Principal?, bot: AbsSender) : Boolean = true

    /**
     * called when no handler has been found
     */
    open fun onNoHandler(update: Update, principal: Principal?, bot: AbsSender) {}

}