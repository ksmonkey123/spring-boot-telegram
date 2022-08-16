package ch.awae.telegram.spring.internal.param

import ch.awae.telegram.spring.api.Principal
import kotlin.reflect.KType

data class TypedPrincipal(val type: KType) : TypedParam<Principal>(type), ParameterMapping