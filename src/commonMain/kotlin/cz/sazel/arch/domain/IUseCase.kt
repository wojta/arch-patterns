package cz.sazel.arch.domain

import cz.sazel.arch.result.Result

/**
 * One domain operation, taking a single [INPUT] and answering with either an [OUTPUT] or a [FAIL]
 * reason. Use [Unit] as [INPUT] for an operation that takes nothing.
 */
fun interface IUseCase<in INPUT, out OUTPUT, out FAIL> {

    operator fun invoke(input: INPUT): Result<OUTPUT, FAIL>
}

/** [IUseCase] whose [invoke] suspends. */
fun interface ISuspendingUseCase<in INPUT, out OUTPUT, out FAIL> {

    suspend operator fun invoke(input: INPUT): Result<OUTPUT, FAIL>
}
