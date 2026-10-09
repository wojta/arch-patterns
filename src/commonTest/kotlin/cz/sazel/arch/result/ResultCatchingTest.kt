package cz.sazel.arch.result

import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

class ResultCatchingTest {

    @Test
    fun `a call that throws is mapped to a failure`() = runTest {
        val result = Result.catching<Int, String>(call = { error("boom") }, onError = { it.message ?: "" })

        result.failReasonOrNull shouldBe "boom"
    }

    @Test
    fun `a call that returns is wrapped as ok`() = runTest {
        Result.catching<Int, String>(call = { 7 }, onError = { "unused" }).dataOrNull shouldBe 7
    }

    @Test
    fun `cancelling the calling job during a suspended call ends it with cancellation`() = runTest {
        val inside = CompletableDeferred<Unit>()
        var outcome: Any? = null
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            outcome = Result.catching<Unit, String>(
                call = { inside.complete(Unit); awaitCancellation() },
                onError = { "mapped" },
            )
        }
        inside.await()

        job.cancel()
        job.join()

        job.isCancelled shouldBe true
        outcome shouldBe null
    }
}
