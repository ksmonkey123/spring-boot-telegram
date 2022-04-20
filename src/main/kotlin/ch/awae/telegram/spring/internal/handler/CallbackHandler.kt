package ch.awae.telegram.spring.internal.handler

import ch.awae.telegram.spring.annotation.Authorized
import ch.awae.telegram.spring.annotation.mapping.OnCallback
import ch.awae.telegram.spring.api.Principal
import ch.awae.telegram.spring.internal.BotControllerBinding
import ch.awae.telegram.spring.internal.param.*
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.objects.Update
import java.util.UUID
import kotlin.reflect.KFunction

class CallbackHandler(
    private val bean: Any,
    private val regex: Regex,
    private val function: KFunction<*>,
    private val parameterMapping: List<ParameterMapping>,
    private val annotation: OnCallback,
    override val classLevelAuth: Authorized?,
    override val functionLevelAuth: Authorized?,
) : Handler {

    override val priority: Int
        get() = annotation.priority

    override fun toString(): String = "onCallback($priority, pattern='${regex.pattern}', function='$function')"

    private fun matchText(update: Update): MatchResult? {
        val data: String? = update.callbackQuery?.data
        return data?.let { regex.matchEntire(it) }
    }

    override fun isApplicable(update: Update): Boolean = matchText(update) != null

    override fun invoke(uuid: UUID, update: Update, principal: Principal?, binding: BotControllerBinding) {
        val match = matchText(update)!!

        val parameters = mutableListOf<Any?>(bean)
        val valueParameters = parameterMapping.map {
            when (it) {
                is IndexedGroup -> runCatching { match.groups[it.index]?.value }.getOrNull()
                is NamedGroup -> runCatching { match.groups[it.name]?.value }.getOrNull()
                is RawUpdate -> update
                is RawMessage -> null
                is RawCallback -> update.callbackQuery
                is TypedPrincipal -> it.filterType(principal)
            }
        }

        parameters.addAll(valueParameters)
        val result = function.call(*parameters.toTypedArray())

        if (!annotation.keepKeyboard) {
            binding.execute(
                EditMessageReplyMarkup(
                    update.callbackQuery.message.chatId.toString(),
                    update.callbackQuery.message.messageId,
                    null,
                    null
                )
            )
        }

        binding.processResponse(uuid, update.callbackQuery.message, result, false)
    }

}