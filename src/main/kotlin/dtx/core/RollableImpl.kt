package dtx.core

public data class AnyOf<T, R>(
    public val rollables: List<Rollable<T, R>>,
    public val predicate: ShouldRoll<T> = ::defaultShouldRoll,
    public val onSelectFun: OnSelect<T, R> = Rollable.Companion::defaultOnSelect
): Rollable<T, R> {

    override fun shouldRoll(target: T): Boolean {
        return predicate.invoke(target)
    }

    override fun onSelect(target: T, result: RollResult<R>) {
        return onSelectFun.invoke(target, result)
    }

    override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

        if (!shouldRoll(target)) {
            return RollResult.Nothing()
        }

        val result = rollables.random().roll(target, otherArgs)
        onSelect(target, result)

        return result
    }
}

public data class AllOf<T, R>(
    public val rollables: List<Rollable<T, R>>,
    public val shouldRollFunc: ShouldRoll<T> = ::defaultShouldRoll,
    public val onSelectFun: OnSelect<T, R> = Rollable.Companion::defaultOnSelect
): Rollable<T, R> {

    override fun shouldRoll(target: T): Boolean {
        return shouldRollFunc(target)
    }

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
    public val shouldRollFunc: ShouldRoll<T> = ::defaultShouldRoll,
    public val onSelectFun: OnSelect<T, R> = Rollable.Companion::defaultOnSelect
): Rollable<T, R> {

    override fun shouldRoll(target: T): Boolean {
        return shouldRollFunc(target)
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
    public val resultSelector: ResultSelector<T, R>,
    public val shouldRollFunc: ShouldRoll<T> = ::defaultShouldRoll,
    public val onSelectFun: OnSelect<T, R> = Rollable.Companion::defaultOnSelect
): Rollable<T, R> {

    override fun shouldRoll(target: T): Boolean {
        return shouldRollFunc(target)
    }

    override fun onSelect(target: T, result: RollResult<R>) {
        return onSelectFun.invoke(target, result)
    }

    override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

        if (!shouldRoll(target)) {
            return RollResult.Nothing()
        }

        val result = RollResult.Single(resultSelector.invoke(target))
        onSelect(target, result)

        return result
    }
}
