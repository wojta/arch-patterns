package cz.sazel.arch.result

@PublishedApi
internal actual fun Throwable.isFatal(): Boolean = false
