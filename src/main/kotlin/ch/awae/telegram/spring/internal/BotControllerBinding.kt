package ch.awae.telegram.spring.internal

import ch.awae.telegram.spring.api.*
import ch.awae.telegram.spring.internal.handler.FallbackHandler
import ch.awae.telegram.spring.internal.handler.Handler
import com.fasterxml.jackson.databind.ObjectMapper
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import java.io.Serializable
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.reflect.jvm.jvmName

class BotControllerBinding(
        private val configuration: IBotCredentials,
        private val handlers: List<Handler>,
        private val fallbackHandler: FallbackHandler?,
        private val telegramBotConfiguration: TelegramBotConfiguration,
) : TelegramLongPollingBot() {

    private val logger = Logger.getLogger(BotControllerBinding::class.jvmName)

    override fun getBotToken(): String = configuration.token
    override fun getBotUsername(): String = configuration.username

    override fun onUpdateReceived(update: Update) {
        try {
            val uuid = UUID.randomUUID()
            val json = kotlin.runCatching { ObjectMapper().writeValueAsString(update) }.getOrDefault(update.toString())
            logger.info("$uuid: processing update $json")
            val principal = extractUserId(update)?.let { telegramBotConfiguration.resolvePrincipal(it) }

            if (!telegramBotConfiguration.onUpdate(update, principal, this)) {
                logger.info("$uuid: skipping processing due to onUpdate result")
                return
            }

            val candidates = handlers.filter { it.isApplicable(update) }.sortedBy { it.priority }
            val authorizedCandidates = candidates.filter { it.isAuthorized(principal) }

            if (authorizedCandidates.size > 1) {
                val sameLevel = authorizedCandidates.filter { it.priority == authorizedCandidates[0].priority }
                logger.warning("$uuid: multiple applicable handlers with same priority found:")
                sameLevel.forEach { logger.warning(" - $it") }
            }

            if (authorizedCandidates.isEmpty() && candidates.isNotEmpty()) {
                logger.warning("$uuid: unauthorized access matching one of the following handlers:")
                authorizedCandidates.forEach { logger.warning("$uuid: - $it") }
                telegramBotConfiguration.onUnauthorizedAccess(update, principal, this)
            } else if (authorizedCandidates.isNotEmpty()) {
                invokeHandler(uuid, authorizedCandidates.first(), update, principal)
            } else if (fallbackHandler != null && fallbackHandler.isAuthorized(principal)) {
                invokeHandler(uuid, fallbackHandler, update, principal)
            } else if (fallbackHandler != null) {
                logger.warning("$uuid: unauthorized access to fallback handler")
                authorizedCandidates.forEach { logger.warning("$uuid: - $it") }
                telegramBotConfiguration.onUnauthorizedAccess(update, principal, this)
            } else {
                logger.info("$uuid: no handler found")
                telegramBotConfiguration.onNoHandler(update, principal, this)
            }
        } catch (e: Exception) {
            logger.log(Level.SEVERE, e.toString(), e)
        }
    }

    private fun invokeHandler(uuid: UUID, handler: Handler, update: Update, principal: Principal?) {
        if (telegramBotConfiguration.onAuthorizedAccess(update, principal, this)) {
            handler.invoke(uuid, update, principal, this)
        } else {
            logger.info("$uuid: skipping processing due to onAuthorizedAccess result")
        }
    }

    override fun <T : Serializable, Method : BotApiMethod<T>> execute(method: Method): T {
        return super.execute(method)
    }

    fun processResponse(uuid: UUID, update: Update, result: Any?, linked: Boolean) {
        val message = (update.message ?: update.callbackQuery?.message)!!

        result?.takeUnless { it is Unit }?.let {
            logger.info("$uuid: sending response: $it")
            val response = when (it) {
                is BotApiMethod<*> -> it
                is Keyboard -> {
                    SendMessage(message.chatId.toString(), it.message).apply {
                        replyMarkup = it.buildMarkup()
                        if (linked) {
                            replyToMessageId = message.messageId
                        }
                    }
                }
                else -> {
                    SendMessage(message.chatId.toString(), it.toString()).apply {
                        if (linked) {
                            replyToMessageId = message.messageId
                        }
                    }
                }
            }
            execute(response)
        }
    }

    private fun extractUserId(update: Update): Long? {
        val user: User? = update.message?.from ?: update.callbackQuery?.from
        return user?.id
    }

}