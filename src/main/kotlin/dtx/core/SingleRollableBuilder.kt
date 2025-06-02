package dtx.core

public open class SingleRollableBuilder<T, R> {

    public var result: R? = null
    public var resultSelector: (() -> R)? = null
    public var shouldRollFunc: (T) -> Boolean = { true }
    public var onSelectFun: (T, RollResult<R>) -> Unit = Rollable.Companion::defaultOnSelect

    public fun onSelect(block: (T, RollResult<R>) -> Unit): SingleRollableBuilder<T, R> = apply {
        onSelectFun = block
    }

    public fun shouldRoll(block: (T) -> Boolean): SingleRollableBuilder<T, R> = apply {
        shouldRollFunc = block
    }

    public fun result(newResult: R): SingleRollableBuilder<T, R> = apply {
        result = newResult
    }

    public fun result(resultFunc: () -> R): SingleRollableBuilder<T, R> = apply {
        resultSelector = resultFunc
    }

    public open fun build(): Rollable<T, R> {

        resultSelector?.let {
            return SingleByFun(it, shouldRollFunc, onSelectFun)
        }

        result?.let {
            return Single(it, shouldRollFunc, onSelectFun)
        }

        throw IllegalStateException("Cannot Build SingleRollable with both null result and resultSelector")
    }
}

public fun <T, R> singleRollable(block: SingleRollableBuilder<T, R>.() -> Unit): Rollable<T, R> {

    val builder = SingleRollableBuilder<T, R>()
    builder.apply(block)

    return builder.build()
}