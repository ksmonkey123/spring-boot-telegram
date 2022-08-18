package ch.awae.telegram.spring.api

enum class ProcessingOutcome {

    /** stopped after onUpdate */
    SKIPPED,

    /** update processing failed at an unknown state */
    FAILED,

    /** handler unauthorized */
    HANDLER_UNAUTHORIZED,

    /** skipped after onAuthorizedAccess */
    HANDLER_SKIPPED,

    /** handler threw an exception */
    HANDLER_FAILED,

    /** handler completed */
    HANDLER_COMPLETED,

    /** fallback handler unauthorized */
    FALLBACK_UNAUTHORIZED,

    /** fallback handler skipped */
    FALLBACK_SKIPPED,

    /** fallback handler threw an exception */
    FALLBACK_FAILED,

    /** fallback handler completed */
    FALLBACK_COMPLETED,

    /** no handler */
    NO_HANDLER,
}