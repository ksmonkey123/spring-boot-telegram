package ch.awae.telegram.spring.internal.handler

import ch.awae.telegram.spring.annotation.Authorized
import ch.awae.telegram.spring.api.Principal
import ch.awae.telegram.spring.internal.BotControllerBinding
import ch.awae.telegram.spring.internal.param.*
import org.telegram.telegrambots.meta.api.objects.Update
import kotlin.reflect.KFunction

class FallbackHandler(
        private val bean: Any,
        private val function: KFunction<*>,
        private val parameterMapping: List<ParameterMapping>,
        override val classLevelAuth: Authorized?,
        override val functionLevelAuth: Authorized?,
) : Handler {

    override val priority = Int.MAX_VALUE

    override fun toString(): String = "fallbackHandler(function='$function')"

    override fun isApplicable(update: Update): Boolean = true

    override fun invoke(update: Update, principal: Principal?, binding: BotControllerBinding) {
        val parameters = mutableListOf<Any?>(bean)
        val valueParameters = parameterMapping.map {
            when (it) {
                is RawUpdate -> update
                is RawMessage -> update.message
                is RawCallback -> update.callbackQuery
                is TypedPrincipal -> it.filterType(principal)
                else -> null
            }
        }
        parameters.addAll(valueParameters)
        function.call(*parameters.toTypedArray())
    }

}
