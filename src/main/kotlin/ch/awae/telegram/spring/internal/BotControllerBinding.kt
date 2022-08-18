package ch.awae.telegram.spring.internal

import ch.awae.telegram.spring.api.*
import ch.awae.telegram.spring.internal.handler.FallbackHandler
import ch.awae.telegram.spring.internal.handler.Handler
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.web.servlet.support.ServletContextApplicationContextInitializer
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import java.io.Serializable
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.reflect.jvm.jvmName

class BotControllerBinding<P : Principal, C : UpdateContext<P>>(
    private val configuration: BotCredentials,
    private val handlers: List<Handler>,
    private val fallbackHandler: FallbackHandler?,
    private val telegramBotConfiguration: TelegramBotConfiguration<P, C>,
) : TelegramLongPollingBot() {

    private val callbackHandler = telegramBotConfiguration.getUpdateCallbackHandler()

    private val logger = Logger.getLogger(BotControllerBinding::class.jvmName)

    override fun getBotToken(): String = configuration.token
    override fun getBotUsername(): String = configuration.username

    private fun test(block: () -> Unit): Throwable? = runCatching(block).exceptionOrNull()

    override fun onUpdateReceived(update: Update) {
        try {
            val result = handleUpdate(update)
            callbackHandler?.onUpdateCompleted(update, result.first, result.second, result.third)
        } catch (e: Exception) {
            logger.log(Level.SEVERE, e.toString(), e)
        }
    }

    private fun handleUpdate(update: Update): Triple<C, ProcessingOutcome, Result<BotApiMethod<out Serializable>?>> {
        val uuid = UUID.randomUUID()
        val json = kotlin.runCatching { ObjectMapper().writeValueAsString(update) }.getOrDefault(update.toString())
        logger.info("$uuid: processing update $json")
        val principal = extractUserId(update)?.let { telegramBotConfiguration.resolvePrincipal(it) }
        val context = telegramBotConfiguration.buildUpdateContext(this, update, principal)

        try {
            test { callbackHandler?.onUpdate(update, context) }?.let {
                logger.info("$uuid: skipping processing due to onUpdate result: $it")
                return Triple(context, ProcessingOutcome.SKIPPED, Result.failure(it))
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
                candidates.forEach { logger.warning("$uuid: - $it") }
                return Triple(context,
                    ProcessingOutcome.HANDLER_UNAUTHORIZED,
                    runCatching { callbackHandler?.onUnauthorizedAccess(update, context) }.map { null })
            } else if (authorizedCandidates.isNotEmpty()) {
                test { callbackHandler?.onAuthorizedAccess(update, context) }?.let {
                    logger.info("$uuid: skipping processing due to onAuthorizedAccess result: $it")
                    return Triple(context, ProcessingOutcome.HANDLER_SKIPPED, Result.failure(it))
                }
                return invokeHandler(uuid, authorizedCandidates.first(), update, principal, context).let {
                    if (it.isFailure) {
                        Triple(context, ProcessingOutcome.HANDLER_FAILED, it)
                    } else {
                        Triple(context, ProcessingOutcome.HANDLER_COMPLETED, it)
                    }
                }
            } else if (fallbackHandler != null && fallbackHandler.isAuthorized(principal)) {
                test { callbackHandler?.onAuthorizedAccess(update, context) }?.let {
                    logger.info("$uuid: skipping processing due to onAuthorizedAccess result: $it")
                    return Triple(context, ProcessingOutcome.FALLBACK_SKIPPED, Result.failure(it))
                }
                return invokeHandler(uuid, fallbackHandler, update, principal, context).let {
                    if (it.isFailure) {
                        Triple(context, ProcessingOutcome.HANDLER_FAILED, it)
                    } else {
                        Triple(context, ProcessingOutcome.HANDLER_COMPLETED, it)
                    }
                }
            } else if (fallbackHandler != null) {
                logger.warning("$uuid: unauthorized access to fallback handler")
                authorizedCandidates.forEach { logger.warning("$uuid: - $it") }
                return Triple(context,
                    ProcessingOutcome.FALLBACK_UNAUTHORIZED,
                    runCatching { callbackHandler?.onUnauthorizedAccess(update, context) }.map { null })
            } else {
                logger.info("$uuid: no handler found")
                return Triple(context,
                    ProcessingOutcome.NO_HANDLER,
                    runCatching { callbackHandler?.onNoHandler(update, context) }.map { null })
            }
        } catch (e: Exception) {
            logger.log(Level.SEVERE, e.toString(), e)
            return Triple(context, ProcessingOutcome.FAILED, Result.failure(e))
        }
    }

    private fun invokeHandler(
        uuid: UUID,
        handler: Handler,
        update: Update,
        principal: P?,
        context: C
    ): Result<BotApiMethod<out Serializable>?> {
        return runCatching { handler.invoke(uuid, update, principal, this, context) }
    }

    override fun <T : Serializable, Method : BotApiMethod<T>> execute(method: Method): T {
        return super.execute(method)
    }

    fun processResponse(uuid: UUID, update: Update, result: Any?, linked: Boolean): BotApiMethod<out Serializable>? {
        val message = (update.message ?: update.callbackQuery?.message)!!

        result?.takeUnless { it is Unit }?.let {
            logger.info("$uuid: sending response: $it")
            val response: BotApiMethod<out Serializable> = when (it) {
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
            return response
        }
        return null
    }

    private fun extractUserId(update: Update): Long? {
        val user: User? = update.message?.from ?: update.callbackQuery?.from
        return user?.id
    }

}