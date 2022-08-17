package ch.awae.telegram.spring.internal.handler

import ch.awae.telegram.spring.annotation.Authorized
import ch.awae.telegram.spring.api.Principal
import ch.awae.telegram.spring.api.UpdateContext
import ch.awae.telegram.spring.internal.BotControllerBinding
import org.telegram.telegrambots.meta.api.objects.Update
import java.util.UUID

interface Handler {
    fun isApplicable(update: Update): Boolean
    fun invoke(uuid: UUID, update: Update, principal: Principal?, binding: BotControllerBinding<*,*>, context: UpdateContext<*>)
    val priority: Int
    val classLevelAuth: Authorized?
    val functionLevelAuth: Authorized?

    fun isAuthorized(principal: Principal?) : Boolean {
        if (principal == null) {
            return classLevelAuth == null && functionLevelAuth == null
        }

        val availableRoles = principal.roles.toList()

        val classResult = classLevelAuth?.let { availableRoles.containsAll(it.roles.toList()) } ?: true
        val funResult = functionLevelAuth?.let { availableRoles.containsAll(it.roles.toList()) } ?: true

        return classResult && funResult
    }
}