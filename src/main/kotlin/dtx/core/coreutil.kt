package dtx.core

internal typealias RollModifier<T> = (T, Double) -> Double
internal typealias BaseDroprate<T> = (T) -> Double
internal typealias OnSelect<T, R> = (T, RollResult<R>) -> Unit
internal typealias ShouldRoll<T> = (T) -> Boolean
internal typealias Roll<T, R> = (T, ArgMap) -> RollResult<R>
internal typealias ResultSelector<T, R> = (T) -> R

internal fun <T> defaultShouldRoll(target: T): Boolean {
    return true
}
