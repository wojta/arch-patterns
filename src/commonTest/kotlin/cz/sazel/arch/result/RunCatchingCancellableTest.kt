package cz.sazel.arch.result

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest

class RunCatchingCancellableTest {

    @Test
    fun `a value comes back as a success`() {
        runCatchingCancellable { 42 }.getOrNull() shouldBe 42
    }

    @Test
    fun `an ordinary exception becomes a failure`() {
        val boom = IllegalStateException("boom")

        runCatchingCancellable { throw boom }.exceptionOrNull() shouldBeSameInstanceAs boom
    }

    @Test
    fun `a cancellation is rethrown rather than captured`() {
        val cancelled = CancellationException("stop")

        val thrown = assertFailsWith<CancellationException> { runCatchingCancellable { throw cancelled } }

        thrown shouldBeSameInstanceAs cancelled
    }

    @Test
    fun `the block runs exactly once on success and on failure`() {
        var runs = 0
        runCatchingCancellable { runs++ }
        runs shouldBe 1

        runCatchingCancellable { runs++; error("boom") }
        runs shouldBe 2
    }

    @Test
    fun `cancelling a job suspended inside the block ends it with cancellation`() = runTest {
        val inside = CompletableDeferred<Unit>()
        var outcome: Any? = null
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            outcome = runCatchingCancellable {
                inside.complete(Unit)
                awaitCancellation()
            }
        }
        inside.await()

        job.cancel()

        job.join()

        job.isCancelled shouldBe true
        outcome shouldBe null
    }
}
