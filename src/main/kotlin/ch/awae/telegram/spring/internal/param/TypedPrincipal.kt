package ch.awae.telegram.spring.internal.param

import ch.awae.telegram.spring.api.Principal
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType

data class TypedPrincipal(val type: KType) : ParameterMapping {
    fun filterType(principal: Principal?) : Principal? = principal?.takeIf { it::class.starProjectedType.isSubtypeOf(type) }
}