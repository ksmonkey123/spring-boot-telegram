package ch.awae.telegram.spring.internal

import ch.awae.telegram.spring.api.Principal

data class AnonymousPrincipal(override val userId : Long) : Principal {
    override val roles: Array<String>
        get() = emptyArray()
}