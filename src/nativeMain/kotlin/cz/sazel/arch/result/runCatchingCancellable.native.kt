package cz.sazel.arch.result

@PublishedApi
internal actual fun Throwable.isFatal(): Boolean = when (this) {
    is OutOfMemoryError -> true
    else -> false
}
