package dtx.core

import dtx.core.OnSelect
import dtx.core.ShouldRoll

public open class ListRollableBuilder<T, R> {

    public var rollables: MutableList<Rollable<T, R>> = mutableListOf()
    public var shouldRollFunc: ShouldRoll<T> = ::defaultShouldRoll
    public var onSelectFun: OnSelect<T, R> = Rollable.Companion::defaultOnSelect

    public fun shouldRoll(block: ShouldRoll<T>): ListRollableBuilder<T, R> {

        shouldRollFunc = block

        return this
    }

    public fun onSelect(block: OnSelect<T, R>): ListRollableBuilder<T, R> {

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
