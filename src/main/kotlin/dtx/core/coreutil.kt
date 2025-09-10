package dtx.core

internal typealias ModifyRoll<T> = (T, Double) -> Double
internal typealias BaseRoll<T> = (T) -> Double
internal typealias OnSelect<T, R> = (T, RollResult<R>) -> Unit
internal typealias ShouldInclude<T> = (T) -> Boolean
internal typealias VetoRoll<T> = (T) -> Boolean
internal typealias OnVetoRoll<T, R> = (T) -> RollResult<R>
internal typealias ResultSelector<T, R> = SingleByFun<T, R>.(T, ArgMap) -> RollResult<R>
internal typealias ItemSelector<T, R> = SingleByFun<T, R>.(T, ArgMap) -> R
internal typealias TransformResult<T, R> = (T, RollResult<R>) -> RollResult<R>
internal typealias OnExhaust<T> = (T) -> Unit
