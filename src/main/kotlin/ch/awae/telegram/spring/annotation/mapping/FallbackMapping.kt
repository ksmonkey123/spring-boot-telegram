package ch.awae.telegram.spring.annotation.mapping

/**
 * marks a function as the fallback handler if no other compatible mapping was found.
 *
 * Each bot may only have one fallback mapping! (note: each bot, not each controller!)
 *
 * Fallback mappings can be authorized. If the use of an update is not authorized for the fallback function,
 * no action will be taken.
 *
 * If a regular mapping has been found, but the user was authorized, the fallback function will not be called!
 * If a fallback function is desired for a specific mapping that is only used when the user is not authorized for
 * any of the other functions, this must be done through the priority value on the other mapping annotations.
 */
@Target(AnnotationTarget.FUNCTION)
annotation class FallbackMapping(
    /**
     * set to 'true' if the result of the method should be sent as a marked response to the incoming message.
     * only applicable for result types that have custom serialization logic (e.g. String or Keyboard).
     *
     * If the incoming update is anything but a Message, this flag is ignored.
     * If a raw BotApiMessage is returned, this flag is ignored.
     */
    val linkResponse: Boolean = false,
)