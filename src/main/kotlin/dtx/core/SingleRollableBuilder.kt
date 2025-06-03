package dtx.core

public open class SingleRollableBuilder<T, R> {

    public var result: R? = null
    public var resultSelector: ResultSelector<T, R>? = null
    public var shouldRollFunc: ShouldRoll<T> = ::defaultShouldRoll
    public var onSelectFunc: OnSelect<T, R> = Rollable.Companion::defaultOnSelect

    public fun onSelect(block: OnSelect<T, R>): SingleRollableBuilder<T, R> = apply {
        onSelectFunc = block
    }

    public fun shouldRoll(block: ShouldRoll<T>): SingleRollableBuilder<T, R> = apply {
        shouldRollFunc = block
    }

    public fun result(newResult: R): SingleRollableBuilder<T, R> = apply {
        result = newResult
    }

    public fun result(resultFunc: ResultSelector<T, R>): SingleRollableBuilder<T, R> = apply {
        resultSelector = resultFunc
    }

    public open fun build(): Rollable<T, R> {

        resultSelector?.let {
            return SingleByFun(it, shouldRollFunc, onSelectFunc)
        }

        result?.let {
            return Single(it, shouldRollFunc, onSelectFunc)
        }

        throw IllegalStateException("SingleRollable must have at least result or resultSelector defined")
    }
}

public fun <T, R> singleRollable(block: SingleRollableBuilder<T, R>.() -> Unit): Rollable<T, R> {

    val builder = SingleRollableBuilder<T, R>()
    builder.apply(block)

    return builder.build()
}
