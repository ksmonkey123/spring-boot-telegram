package ch.awae.telegram.spring.annotation.param

/**
 * Marks a BotController function parameter as a raw incoming message type.
 * This can be used if the function needs more detailed information about the
 * incoming message.
 *
 * Not all supported types are applicable on all handler types.
 * For example, the 'Message' type is only available on @OnMessage handlers.
 * Not-null raw parameters are only allowed on handlers where the presence
 * of the field is guaranteed. Nullable parameters are allowed on all handlers and
 * are simply passed 'null' if the field is not available. This is especially
 * useful on handlers used for multiple different event types.
 *
 * Due to limitations in reflection capabilities, all parameters declared in Java code
 * are treated as non-null. This can be overridden using the parameter 'optional'.
 * When set to 'true', the parameter is treated as optional no matter the parameter nullability.
 * This should only ever be used in java code.
 *
 * @see org.telegram.telegrambots.meta.api.objects.Update
 * @see org.telegram.telegrambots.meta.api.objects.Message
 * @see org.telegram.telegrambots.meta.api.objects.CallbackQuery
 *
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Raw(val optional: Boolean = false)
