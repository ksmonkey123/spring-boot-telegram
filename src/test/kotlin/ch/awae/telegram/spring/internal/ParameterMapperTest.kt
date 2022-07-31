package ch.awae.telegram.spring.internal

import ch.awae.telegram.spring.annotation.mapping.FallbackMapping
import ch.awae.telegram.spring.annotation.mapping.OnCallback
import ch.awae.telegram.spring.annotation.mapping.OnMessage
import ch.awae.telegram.spring.annotation.param.Group
import ch.awae.telegram.spring.annotation.param.Raw
import ch.awae.telegram.spring.annotation.param.Text
import ch.awae.telegram.spring.api.Principal
import ch.awae.telegram.spring.api.UpdateContext
import ch.awae.telegram.spring.internal.param.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import java.util.stream.Stream
import javax.security.auth.callback.Callback
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.typeOf

internal class ParameterMapperTest {

    data class MockParameter(
        override val name: String?,
        override val type: KType,
        private val annotation: Annotation?,
        override val isVararg: Boolean = false,
        override val index: Int = 0,
        override val isOptional: Boolean = false,
        override val kind: KParameter.Kind = KParameter.Kind.VALUE
    ) : KParameter {
        override val annotations: List<Annotation>
            get() = annotation?.let { listOf(it) } ?: emptyList()
    }

    @ParameterizedTest
    @MethodSource
    fun test_getParameterMapping(
        param: KParameter, mappingAnnotation: Annotation, expected: ParameterMapping?
    ) {
        try {
            val mapping = ParameterMapper.getParameterMapping(param, mappingAnnotation)
            assertEquals(expected, mapping)
        } catch (e: Exception) {
            if (expected != null) {
                fail<Unit>("unexpected error", e)
            } else {
                assertEquals(InitializationException::class, e::class)
            }
        }
    }

    companion object {

        @JvmStatic
        fun test_getParameterMapping(): Stream<Arguments> =
            listOf(OnMessage("test"), OnCallback("test"), FallbackMapping()).flatMap { mapping ->
                listOf(
                    // @Text annotation yields IndexedGroup(0) on String and String? parameters, other types yield error.
                    Arguments.of(MockParameter(null, typeOf<String>(), Text()), mapping, IndexedGroup(0)),
                    Arguments.of(MockParameter(null, typeOf<String?>(), Text()), mapping, IndexedGroup(0)),
                    Arguments.of(MockParameter(null, typeOf<Any>(), Text()), mapping, null),
                    // @Group() annotation yields IndexedGroup(0) on String and String? parameters, other types yield error.
                    Arguments.of(MockParameter(null, typeOf<String>(), Group()), mapping, IndexedGroup(0)),
                    Arguments.of(MockParameter(null, typeOf<String?>(), Group()), mapping, IndexedGroup(0)),
                    Arguments.of(MockParameter(null, typeOf<Any>(), Group()), mapping, null),
                    // @Group(id=1) annotation yields IndexedGroup(1) on String and String? parameters, other types yield error.
                    Arguments.of(MockParameter(null, typeOf<String>(), Group(id = 1)), mapping, IndexedGroup(1)),
                    Arguments.of(MockParameter(null, typeOf<String?>(), Group(id = 1)), mapping, IndexedGroup(1)),
                    Arguments.of(MockParameter(null, typeOf<Any>(), Group(id = 1)), mapping, null),
                    // @Group(id=-1) annotation yields error.
                    Arguments.of(MockParameter(null, typeOf<String>(), Group(id = -1)), mapping, null),
                    Arguments.of(MockParameter(null, typeOf<String?>(), Group(id = -1)), mapping, null),
                    Arguments.of(MockParameter(null, typeOf<Any>(), Group(id = -1)), mapping, null),
                    // @Group(name="tag") annotation yields NamedGroup("tag") on String and String? parameters, other types yield error.
                    Arguments.of(MockParameter(null, typeOf<String>(), Group("tag")), mapping, NamedGroup("tag")),
                    Arguments.of(MockParameter(null, typeOf<String?>(), Group("tag")), mapping, NamedGroup("tag")),
                    Arguments.of(MockParameter(null, typeOf<Any>(), Group("tag")), mapping, null),
                    // @Raw annotation on different types:
                    // -> Update => RawUpdate
                    Arguments.of(MockParameter(null, typeOf<Update>(), Raw()), mapping, RawUpdate),
                    Arguments.of(MockParameter(null, typeOf<Update?>(), Raw()), mapping, RawUpdate),
                    // -> Message => RawMessage (non-null only on @OnMessage),
                    Arguments.of(MockParameter(null, typeOf<Message>(), Raw()), mapping, (mapping as? OnMessage)?.let{RawMessage}),
                    Arguments.of(MockParameter(null, typeOf<Message>(), Raw(true)), mapping, RawMessage),
                    Arguments.of(MockParameter(null, typeOf<Message?>(), Raw()), mapping, RawMessage),
                    // -> CallbackQuery => RawCallback (non-null only on @OnCallback)
                    Arguments.of(MockParameter(null, typeOf<CallbackQuery>(), Raw()), mapping, (mapping as? OnCallback)?.let{RawCallback}),
                    Arguments.of(MockParameter(null, typeOf<CallbackQuery>(), Raw(true)), mapping, RawCallback),
                    Arguments.of(MockParameter(null, typeOf<CallbackQuery?>(), Raw()), mapping, RawCallback),
                    // unannotated parameters of different types:
                    // -> String => named group
                    Arguments.of(MockParameter("tag", typeOf<String>(), null), mapping, NamedGroup("tag")),
                    Arguments.of(MockParameter("tag", typeOf<String?>(), null), mapping, NamedGroup("tag")),
                    // -> unnamed String => error
                    Arguments.of(MockParameter(null, typeOf<String>(), null), mapping, null),
                    Arguments.of(MockParameter(null, typeOf<String?>(), null), mapping, null),
                    // -> Principal => TypedPrincipal
                    Arguments.of(MockParameter("user", typeOf<Principal>(), null), mapping, TypedPrincipal(typeOf<Principal>())),
                    Arguments.of(MockParameter("user", typeOf<Principal?>(), null), mapping, TypedPrincipal(typeOf<Principal?>())),
                    // -> UpdateContext => ExplicitContext
                    Arguments.of(MockParameter("context", typeOf<UpdateContext>(), null), mapping, ExplicitContext),
                    Arguments.of(MockParameter("context", typeOf<UpdateContext?>(), null), mapping, ExplicitContext),
                )
            }.stream()
    }

}