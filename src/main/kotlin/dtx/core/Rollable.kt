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

        public fun <T, R> AnyOf(
            rollables: List<Rollable<T, R>>,
            predicate: (T) -> Boolean = { true },
            onSelectFun: (T, RollResult<R>) -> Unit = ::defaultOnSelect
        ): Rollable<T, R> {
            return dtx.core.AnyOf(rollables, predicate, onSelectFun)
        }

        public fun <T, R> AllOf(
            rollables: List<Rollable<T, R>>,
            predicate: (T) -> Boolean = { true },
            onSelectFun: (T, RollResult<R>) -> Unit = ::defaultOnSelect
        ): Rollable<T, R> {
            return dtx.core.AllOf(rollables, predicate, onSelectFun)
        }

        public fun <T, R> Single(
            result: R,
            predicate: (T) -> Boolean = { true },
            onSelectFun: (T, RollResult<R>) -> Unit = ::defaultOnSelect
        ): Rollable<T, R> {
            return dtx.core.Single(result, predicate, onSelectFun)
        }

        public fun <T, R> SingleByFun(
            resultSelector: () -> R,
            predicate: (T) -> Boolean = { true },
            onSelectFun: (T, RollResult<R>) -> Unit = ::defaultOnSelect
        ): Rollable<T, R> {
            return dtx.core.SingleByFun(resultSelector, predicate, onSelectFun)
        }
    }
}
