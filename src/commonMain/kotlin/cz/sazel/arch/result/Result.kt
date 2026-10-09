package cz.sazel.arch.result

/**
 * A discriminated union representing either a successful result ([Ok]) or a failed result ([Fail]).
 *
 * @param OK the type of the success value
 * @param FAIL the type of the failure reason
 */
sealed interface Result<out OK, out FAIL> {

    /**
     * Represents a successful result.
     *
     * @param OK the type of the success value
     * @property data the success value
     */
    data class Ok<OK>(val data: OK) : Result<OK, Nothing>

    /**
     * Represents a failed result.
     *
     * @param FAIL the type of the failure reason
     * @property reason the failure reason
     */
    data class Fail<FAIL>(val reason: FAIL) : Result<Nothing, FAIL>

    /** `true` if this is an [Ok] result. */
    val isOk: Boolean
        get() = this is Ok

    /** The success data if this is [Ok], or `null` otherwise. */
    val dataOrNull: OK?
        get() = if (isOk) (this as Ok).data else null

    /** The failure reason if this is [Fail], or `null` otherwise. */
    val failReasonOrNull: FAIL?
        get() = if (!isOk) (this as Fail).reason else null

    companion object {

        /**
         * Wraps a suspending [call] in a [Result], mapping any thrown exception to [Fail] via [onError].
         */
        suspend fun <OK, FAIL> catching(
                call: suspend () -> OK,
                onError: (Throwable) -> FAIL
        ): Result<OK, FAIL> = runCatchingCancellable {
            Ok(call())
        }.getOrElse {
            Fail(onError(it))
        }

        /**
         * Unwraps the success value, or throws an exception produced by [call] for a [Fail] result.
         */
        fun <OK, FAIL> Result<OK, FAIL>.dataOrThrow(
                call: (FAIL) -> Throwable
        ): OK = when (this) {
            is Ok -> data
            is Fail -> throw call(reason)
        }

        /**
         * Transforms both paths: maps [Ok] via [onSuccess] and [Fail] via [onFailure].
         */
        inline fun <PREV_OK, PREV_FAIL, NEW_OK, NEW_FAIL>
                Result<PREV_OK, PREV_FAIL>.map(
                onSuccess: (value: PREV_OK) -> NEW_OK,
                onFailure: (error: PREV_FAIL) -> NEW_FAIL
        ): Result<NEW_OK, NEW_FAIL> = when (this) {
            is Ok -> Ok(onSuccess(data))
            is Fail -> Fail(onFailure(reason))
        }

        /**
         * Transforms the [Ok] value via [transform], leaving [Fail] unchanged.
         */
        inline fun <PREV_OK, FAIL, NEW_OK>
                Result<PREV_OK, FAIL>.mapOk(
                transform: (value: PREV_OK) -> NEW_OK
        ): Result<NEW_OK, FAIL> = when (this) {
            is Ok -> Ok(transform(data))
            is Fail -> this
        }

        /**
         * Transforms the [Fail] reason via [transform], leaving [Ok] unchanged.
         */
        inline fun <PREV_FAIL, OK, NEW_FAIL>
                Result<OK, PREV_FAIL>.mapFail(
                transform: (error: PREV_FAIL) -> NEW_FAIL
        ): Result<OK, NEW_FAIL> = when (this) {
            is Ok -> this
            is Fail -> Fail(transform(reason))
        }

        /**
         * Reduces this result to a single value by applying [onSuccess] or [onFailure].
         */
        inline fun <PREV_OK, PREV_FAIL, R>
                Result<PREV_OK, PREV_FAIL>.fold(
                onSuccess: (value: PREV_OK) -> R,
                onFailure: (error: PREV_FAIL) -> R
        ): R = when (this) {
            is Ok -> onSuccess(data)
            is Fail -> onFailure(reason)
        }

        /**
         * Chains a (possibly suspending) operation on the [Ok] path; propagates [Fail] unchanged.
         *
         * Accepts both suspend and non-suspend lambdas, making this the single flat-map for the success path.
         */
        suspend fun <PREV_OK, NEW_OK, FAIL>
                Result<PREV_OK, FAIL>.onOk(
                onSuccess: suspend (value: PREV_OK) -> Result<NEW_OK, FAIL>
        ): Result<NEW_OK, FAIL> = when (this) {
            is Ok -> onSuccess(data)
            is Fail -> this
        }

        /**
         * Chains a (possibly suspending) operation on the [Fail] path; propagates [Ok] unchanged.
         *
         * Accepts both suspend and non-suspend lambdas, making this the single flat-map for the failure path.
         */
        suspend fun <OK, PREV_FAIL, NEW_FAIL>
                Result<OK, PREV_FAIL>.onFail(
                onFailure: suspend (error: PREV_FAIL) -> Result<OK, NEW_FAIL>
        ): Result<OK, NEW_FAIL> = when (this) {
            is Ok -> this
            is Fail -> onFailure(reason)
        }
    }
}

/** Alias for a [Result] with a [Unit] success value. */
typealias UnitResult<FAIL> = Result<Unit, FAIL>
