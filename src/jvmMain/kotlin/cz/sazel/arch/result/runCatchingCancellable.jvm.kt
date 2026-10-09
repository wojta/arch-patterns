package cz.sazel.arch.result

@PublishedApi
internal actual fun Throwable.isFatal(): Boolean = when (this) {
    is InterruptedException -> {
        Thread.currentThread().interrupt()
        true
    }
    is VirtualMachineError, is ThreadDeath, is LinkageError -> true
    else -> false
}
