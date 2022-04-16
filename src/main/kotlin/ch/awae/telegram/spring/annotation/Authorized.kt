package ch.awae.telegram.spring.annotation

/**
 * marks a BotController class or function as authorized.
 *
 * a handler function can only be called when the user has
 * all the roles provided in the list.
 *
 * If both the class and the function are annotated, the authorization
 * is checked in 2 steps: first the class-level authorization is performed,
 * then the function-level authorization.
 *
 * annotations without any roles simply require a user principal to exist.
 */
@Target(AnnotationTarget.CLASS,
        AnnotationTarget.FUNCTION)
annotation class Authorized(vararg val roles: String)