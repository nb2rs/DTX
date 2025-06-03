package dtx.core

public interface Rollable<T, R> {

    public fun shouldRoll(target: T): Boolean

    public fun onSelect(target: T, result: RollResult<R>): Unit {
        return defaultOnSelect(target, result)
    }

    public fun getBaseDropRate(target: T): Double {
        return defaultGetBaseDropRate(target)
    }

    public fun roll(target: T, otherArgs: ArgMap = ArgMap.Empty): RollResult<R>

    public companion object {

        public fun <T, R> defaultOnSelect(target: T, result: RollResult<R>): Unit {
            return Unit
        }

        public fun <T> defaultGetBaseDropRate(target: T): Double {
            return 0.0
        }

        public data object EmptyRollable: Rollable<Any?, Any?> {

            override fun shouldRoll(target: Any?): Boolean {
                return false
            }

            override fun roll(target: Any?, otherArgs: ArgMap): RollResult<Any?> {
                return RollResult.Companion.Nothing()
            }

            override fun getBaseDropRate(target: Any?): Double {
                return 0.0
            }
        }

        public fun <T, R> Empty(): Rollable<T, R> {
            return EmptyRollable as Rollable<T, R>
        }

        public fun <T, R> AnyOf(
            rollables: List<Rollable<T, R>>,
            predicate: ShouldRoll<T> = ::defaultShouldRoll,
            onSelectFun: OnSelect<T, R> = ::defaultOnSelect
        ): Rollable<T, R> {
            return dtx.core.AnyOf(rollables, predicate, onSelectFun)
        }

        public fun <T, R> AllOf(
            rollables: List<Rollable<T, R>>,
            predicate: ShouldRoll<T> = ::defaultShouldRoll,
            onSelectFun: OnSelect<T, R> = ::defaultOnSelect
        ): Rollable<T, R> {
            return dtx.core.AllOf(rollables, predicate, onSelectFun)
        }

        public fun <T, R> Single(
            result: R,
            predicate: ShouldRoll<T> = ::defaultShouldRoll,
            onSelectFun: OnSelect<T, R> = ::defaultOnSelect
        ): Rollable<T, R> {
            return dtx.core.Single(result, predicate, onSelectFun)
        }

        public fun <T, R> SingleByFun(
            resultSelector: ResultSelector<T, R>,
            predicate: ShouldRoll<T> = ::defaultShouldRoll,
            onSelectFun: OnSelect<T, R> = ::defaultOnSelect
        ): Rollable<T, R> {
            return dtx.core.SingleByFun(resultSelector, predicate, onSelectFun)
        }
    }
}
