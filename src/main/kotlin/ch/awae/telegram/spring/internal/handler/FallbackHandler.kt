package ch.awae.telegram.spring.internal.handler

import ch.awae.telegram.spring.annotation.Authorized
import ch.awae.telegram.spring.annotation.mapping.FallbackMapping
import ch.awae.telegram.spring.api.Principal
import ch.awae.telegram.spring.api.UpdateContext
import ch.awae.telegram.spring.internal.BotControllerBinding
import ch.awae.telegram.spring.internal.ParameterMapper
import ch.awae.telegram.spring.internal.param.*
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.objects.Update
import java.io.Serializable
import java.util.UUID
import kotlin.reflect.KFunction

class FallbackHandler(
        private val bean: Any,
        private val function: KFunction<*>,
        override val parameterMapping: List<ParameterMapping>,
        private val annotation: FallbackMapping,
        override val classLevelAuth: Authorized?,
        override val functionLevelAuth: Authorized?,
) : Handler {

    override val priority = Int.MAX_VALUE

    override fun toString(): String = "fallbackHandler(function='$function')"

    override fun isApplicable(update: Update): Boolean = true

    override fun invoke(uuid: UUID, update: Update, principal: Principal?, binding: BotControllerBinding<*,*>, context: UpdateContext<*>): BotApiMethod<out Serializable>? {
        val parameters = ParameterMapper.buildParameterList(parameterMapping, bean, context, null)
        val result = function.call(*parameters.toTypedArray())
        return binding.processResponse(uuid, update, result, annotation.linkResponse)
    }

}
