package dtx.core

public open class ListRollableBuilder<T, R> {

    public var rollables: MutableList<Rollable<T, R>> = mutableListOf()

    public var shouldRollFunc: (T) -> Boolean = { true }

    public var onSelectFun: (T, RollResult<R>) -> Unit = Rollable.Companion::defaultOnSelect

    public fun shouldRoll(block: (T) -> Boolean): ListRollableBuilder<T, R> {

        shouldRollFunc = block

        return this
    }

    public fun onSelect(block: (T, RollResult<R>) -> Unit): ListRollableBuilder<T, R> {

        onSelectFun = block

        return this
    }

    public fun add(rollable: Rollable<T, R>): ListRollableBuilder<T, R> {

        rollables.add(rollable)

        return this
    }

    public fun add(block: SingleRollableBuilder<T, R>.() -> Unit): ListRollableBuilder<T, R> {
        return add(singleRollable(block))
    }

    public fun add(item: R): ListRollableBuilder<T, R> {
        return add(Single(item))
    }

    public fun build(): AllOf<T, R> {
        return AllOf<T, R>(rollables, shouldRollFunc, onSelectFun)
    }
}

public fun <T, R> listRollable(block: ListRollableBuilder<T, R>.() -> Unit): Rollable<T, R> {

    val builder = ListRollableBuilder<T, R>()
    builder.apply(block)

    return builder.build()
}
