package cz.sazel.arch.result

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.cancellation.CancellationException

/**
 * Like [runCatching], but rethrows [CancellationException] and fatal errors (see [isFatal]),
 * so a cancelled coroutine is not turned into an ordinary failure.
 */
@OptIn(ExperimentalContracts::class)
@Suppress("TooGenericExceptionCaught", "InstanceOfCheckForException")
inline fun <T> runCatchingCancellable(block: () -> T): kotlin.Result<T> {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return try {
        kotlin.Result.success(block())
    } catch (e: Throwable) {
        if (e is CancellationException || e.isFatal()) throw e
        kotlin.Result.failure(e)
    }
}

/** Errors that must never be captured into a result: JVM VM errors and interrupts, nothing on JS. */
@PublishedApi
internal expect fun Throwable.isFatal(): Boolean
