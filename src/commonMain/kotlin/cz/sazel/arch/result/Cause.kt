package cz.sazel.arch.result

/**
 * An exception flattened into plain fields.
 *
 * A failure reason travels across module and layer boundaries, and a [Throwable] does not survive
 * that crossing — it cannot be serialized and it means nothing to a presentation layer. [Cause]
 * keeps what a developer needs to read afterwards and nothing else.
 *
 * @property type simple class name of the exception
 * @property message the exception message, when it had one
 * @property stackTrace the stack trace, already rendered as text
 */
data class Cause(
    val type: String,
    val message: String?,
    val stackTrace: String,
) {
    companion object {
        /** Flattens [error] into a [Cause]. */
        fun of(error: Throwable) = Cause(
            type = error::class.simpleName ?: "Throwable",
            message = error.message,
            stackTrace = error.stackTraceToString(),
        )
    }
}
