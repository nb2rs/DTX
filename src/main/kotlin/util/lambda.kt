package util

internal fun <T> NoTransform(): (T) -> T = { it }