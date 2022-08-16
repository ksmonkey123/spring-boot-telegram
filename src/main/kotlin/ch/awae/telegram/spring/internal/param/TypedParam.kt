package ch.awae.telegram.spring.internal.param

import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType

sealed class TypedParam<T>(private val type: KType) {

    fun filterType(argument: T?) : T? = argument?.takeIf { it::class.starProjectedType.isSubtypeOf(type) }

}
