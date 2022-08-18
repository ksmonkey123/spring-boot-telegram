package ch.awae.telegram.spring.api

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.bots.AbsSender
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

@ComponentScan("ch.awae.telegram.spring")
abstract class TelegramBotConfiguration<P: Principal, C: UpdateContext<P>> {

    @Bean
    open fun getApi() = TelegramBotsApi(DefaultBotSession::class.java)

    abstract fun getBotCredentials(botName: String) : BotCredentials

    abstract fun resolvePrincipal(userId: Long) : P?

    /**
     * construct a request context. the default implementation constructs a SimpleContext
     */
    abstract fun buildUpdateContext(bot: AbsSender, update: Update, principal: P?): C

    open fun getUpdateCallbackHandler(): UpdateCallbackHandler<P,C>? = null
}