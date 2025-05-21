package dtx.core

public interface Rollable<T, R> {

    public fun shouldRoll(target: T): Boolean = true

    public fun onSelect(target: T, result: RollResult<R>): Unit {
        return defaultOnSelect(target, result)
    }

    public fun getBaseDropRate(target: T): Double {
        return defaultGetBaseDropRate(target)
    }

    public fun roll(target: T, otherArgs: ArgMap = ArgMap.Empty): RollResult<R>

    public companion object {

        public fun <T, R> defaultOnSelect(target: T, result: RollResult<R>): Unit = Unit

        public fun <T> defaultGetBaseDropRate(target: T): Double = 0.0

        public data object Empty: Rollable<Any?, Any?> {

            override fun roll(target: Any?, otherArgs: ArgMap): RollResult<Any?> {
                return RollResult.Companion.Nothing()
            }

            override fun getBaseDropRate(target: Any?): Double {
                return 0.0
            }
        }

        public fun <T, R> Empty(): Rollable<T, R> {
            return Empty as Rollable<T, R>
        }
    }

    public data class ListOf<T, R>(
        public val rollables: List<Rollable<T, R>>,
        public val predicate: (T) -> Boolean = { true },
        public val onSelectFun: (T, RollResult<R>) -> Unit = ::defaultOnSelect
    ): Rollable<T, R> {

        override fun shouldRoll(target: T): Boolean = predicate(target)

        override fun onSelect(target: T, result: RollResult<R>) {
            return onSelectFun.invoke(target, result)
        }

        override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

            if (!shouldRoll(target)) {
                return RollResult.Nothing()
            }

            val results = buildList {

                rollables.forEach { rollable ->
                    add(rollable.roll(target, otherArgs))
                }
            }

            val result = results.flattenToList()
            onSelect(target, result)

            return result
        }
    }

    public data class Single<T, R>(
        public val result: R,
        public val predicate: (T) -> Boolean = { true },
        public val onSelectFun: (T, RollResult<R>) -> Unit = ::defaultOnSelect
    ): Rollable<T, R> {

        override fun shouldRoll(target: T): Boolean {
            return predicate(target)
        }

        override fun onSelect(target: T, result: RollResult<R>) {
            return onSelectFun.invoke(target, result)
        }

        override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

            if (!shouldRoll(target)){
                return RollResult.Nothing()
            }

            val result = RollResult.Single(result)
            onSelect(target, result)

            return result
        }
    }

    public class SingleByFun<T, R>(
        public val resultSelector: () -> R,
        public val predicate: (T) -> Boolean = { true },
        public val onSelectFun: (T, RollResult<R>) -> Unit = ::defaultOnSelect
    ): Rollable<T, R> {

        override fun shouldRoll(target: T): Boolean = predicate(target)

        override fun onSelect(target: T, result: RollResult<R>) {
            return onSelectFun.invoke(target, result)
        }

        override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

            if (!shouldRoll(target)) {
                return RollResult.Nothing()
            }

            val result = RollResult.Single(resultSelector.invoke())
            onSelect(target, result)

            return result
        }
    }
}

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
            return Rollable.SingleByFun(it, shouldRollFunc, onSelectFun)
        }

        result?.let {
            return Rollable.Single(it, shouldRollFunc, onSelectFun)
        }

        throw IllegalStateException("Cannot Build SingleRollable with both null result and resultSelector")
    }
}

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
        return add(Rollable.Single(item))
    }

    public fun build(): Rollable.ListOf<T, R> {
        return Rollable.ListOf<T, R>(rollables, shouldRollFunc, onSelectFun)
    }
}

public fun <T, R> singleRollable(block: SingleRollableBuilder<T, R>.() -> Unit): Rollable<T, R> {

    val builder = SingleRollableBuilder<T, R>()
    builder.apply(block)

    return builder.build()
}


public fun <T, R> listRollable(block: ListRollableBuilder<T, R>.() -> Unit): Rollable<T, R> {

    val builder = ListRollableBuilder<T, R>()
    builder.apply(block)

    return builder.build()
}