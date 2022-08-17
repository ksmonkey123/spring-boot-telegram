package ch.awae.telegram.spring.internal.param

import ch.awae.telegram.spring.api.UpdateContext
import kotlin.reflect.KType

data class TypedContext(val type: KType) : TypedParam<UpdateContext<*>>(type), ParameterMapping