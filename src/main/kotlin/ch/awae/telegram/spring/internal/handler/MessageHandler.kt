package ch.awae.telegram.spring.internal.handler

import ch.awae.telegram.spring.annotation.Authorized
import ch.awae.telegram.spring.annotation.mapping.OnMessage
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

class MessageHandler(
    private val bean: Any,
    private val regex: Regex,
    private val function: KFunction<*>,
    override val parameterMapping: List<ParameterMapping>,
    private val annotation: OnMessage,
    override val classLevelAuth: Authorized?,
    override val functionLevelAuth: Authorized?,
) : Handler {

    override val priority: Int
        get() = annotation.priority

    override fun toString(): String = "onMessage($priority, pattern='${regex.pattern}', function='$function')"

    private fun matchText(update: Update): MatchResult? {
        val text: String? = update.message?.text
        return text?.let { regex.matchEntire(it) }
    }

    override fun isApplicable(update: Update): Boolean = matchText(update) != null

    override fun invoke(uuid: UUID, update: Update, principal: Principal?, binding: BotControllerBinding<*,*>, context: UpdateContext<*>): BotApiMethod<out Serializable>?{
        val parameters = ParameterMapper.buildParameterList(parameterMapping, bean, context, matchText(update))
        val result = function.call(*parameters.toTypedArray())
        return binding.processResponse(uuid, update, result, annotation.linkResponse)
    }

}