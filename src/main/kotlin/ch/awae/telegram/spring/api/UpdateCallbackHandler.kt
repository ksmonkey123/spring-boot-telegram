package ch.awae.telegram.spring.api

import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.objects.Update
import java.io.Serializable

open class UpdateCallbackHandler<P: Principal, C: UpdateContext<P>> {

    /**
     * called when a new update is being processed. this is called at the very beginning, before a handler has been
     * determined and before any authorization has been performed. The user principal has already been established
     * at this point, so it is provided as well.
     *
     * This function also serves as a filter for the further execution: if an exception is thrown, the processing of
     * the update is terminated.
     */
    open fun onUpdate(update: Update, context: C) {}


    /**
     * called whenever an authorization error occurs - i.e. no authorized handler was found for an update
     */
    open fun onUnauthorizedAccess(update: Update, context: C) {}

    /**
     * called when the handler for a message has been determined and authorization has been verified.
     * The call to this function is performed before the chosen handler is called.
     *
     * This function also serves as a filter for the handler execution: if an exception is thrown, the processing of
     * this update is terminated and no handlers are called.
     */
    open fun onAuthorizedAccess(update: Update, context: C) {}

    /**
     * called when no handler has been found
     */
    open fun onNoHandler(update: Update, context: C) {}

    /**
     * called when the update processing is finished, just before a potential response has been sent
     */
    open fun onUpdateCompleted(updateUpdate: Update, context: C, outcome: ProcessingOutcome, result: Result<BotApiMethod<out Serializable>?>) {}
}