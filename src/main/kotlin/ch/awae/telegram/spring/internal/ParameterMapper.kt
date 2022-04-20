package ch.awae.telegram.spring.internal

import ch.awae.telegram.spring.annotation.mapping.OnCallback
import ch.awae.telegram.spring.annotation.mapping.OnMessage
import ch.awae.telegram.spring.annotation.param.Group
import ch.awae.telegram.spring.annotation.param.Raw
import ch.awae.telegram.spring.annotation.param.Text
import ch.awae.telegram.spring.api.Principal
import ch.awae.telegram.spring.internal.param.*
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import kotlin.reflect.KParameter
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.typeOf

object ParameterMapper {

    fun getParameterMapping(param: KParameter, mappingAnnotation: Annotation): ParameterMapping {
        val name = param.name
        val type = param.type

        return when (val annotation = param.annotations.firstOrNull()) {
            is Text -> when(type) {
                typeOf<String>(), typeOf<String?>() -> IndexedGroup(0)
                else -> throw InitializationException("@Text is only supported on String and String? parameters")
            }
            is Group -> when (type) {
                typeOf<String>(), typeOf<String?>() -> groupFromAnnotation(annotation)
                else -> throw InitializationException("@Group is only supported on String and String? parameters")
            }
            is Raw -> when (type) {
                // nonnull raw parameters are not always allowed
                typeOf<Update>() -> RawUpdate
                typeOf<Message>() -> if (mappingAnnotation is OnMessage) RawMessage else throw InitializationException("no raw message available")
                typeOf<CallbackQuery>() -> if (mappingAnnotation is OnCallback) RawCallback else throw InitializationException(
                    "no raw callbackQuery available"
                )
                // optional raw parameters are always allowed
                typeOf<Update?>() -> RawUpdate
                typeOf<Message?>() -> RawMessage
                typeOf<CallbackQuery?>() -> RawCallback
                else -> throw InitializationException("could not determine parameter mapping for parameter $param")
            }
            null -> when {
                type.isSubtypeOf(typeOf<String?>()) && name != null -> NamedGroup(name)
                type.isSubtypeOf(typeOf<Principal?>()) -> TypedPrincipal(type)
                else -> throw InitializationException("could not determine parameter mapping for parameter $param")
            }
            else -> throw InitializationException("could not determine parameter mapping for parameter $param")
        }
    }

    private fun groupFromAnnotation(group: Group) : ParameterMapping {
        return if (group.name.isNotEmpty())
            NamedGroup(group.name)
        else if (group.id >= 0)
            IndexedGroup(group.id)
        else
            throw InitializationException("invalid @Group annotation: name must be non-empty or index >= 0")
    }

    fun buildParameterList(
        parameterMapping: List<ParameterMapping>,
        bean: Any, // the @BotController bean
        principal: Principal?, // the user principal
        update: Update, // the raw Update object. message and callbackQuery are extracted from this
        match: MatchResult?, // the match result where regex groups should be extracted from
    ) : List<Any?> {

        val parameters = mutableListOf<Any?>(bean)
        val valueParameters = parameterMapping.map {
            when (it) {
                is IndexedGroup -> runCatching { match?.groups?.get(it.index)?.value }.getOrNull()
                is NamedGroup -> runCatching { match?.groups?.get(it.name)?.value }.getOrNull()
                is RawUpdate -> update
                is RawMessage -> update.message
                is RawCallback -> update.callbackQuery
                is TypedPrincipal -> it.filterType(principal)
            }
        }

        parameters.addAll(valueParameters)
        return parameters
    }

}