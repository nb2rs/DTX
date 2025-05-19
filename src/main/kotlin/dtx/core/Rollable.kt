package dtx.core

/**
 * Interface representing an entity that can perform a "roll" operation to produce a [RollResult].
 *
 * @param T The type of the target object for which the roll is performed.
 * @param R The type of the result produced by the roll operation.
 */
public interface Rollable<T, R> {

    /**
     * Returns whether or not the [target] is eligible to roll for this drop
     */
    public fun shouldRoll(target: T): Boolean = true

    /**
     * This should be called when a result is selected from this [Rollable].
     *
     * @param target The target object for which the selection was made.
     * @param result The result of the selection.
     */
    public fun onSelect(target: T, result: RollResult<R>): Unit {
        return defaultOnSelect(target, result)
    }

    /**
     * Returns the base drop rate for the given target.
     *
     * @param target The target object for which to get the base drop rate.
     * @return [Double]
     */
    public fun getBaseDropRate(target: T): Double {
        return defaultGetBaseDropRate(target)
    }

    /**
     * Performs a roll for the given target and returns the result.
     *
     * @param target The target object for which to perform the roll.
     * @param otherArgs Additional arguments for the roll operation.
     * @return [RollResult]
     */
    public fun roll(target: T, otherArgs: ArgMap = ArgMap.Empty): RollResult<R>

    /**
     * Companion object containing utility functions and singleton instances.
     */
    public companion object {


        /**
         * Default implementation for the onSelect function that does nothing.
         *
         * @param [T] The type of the [target] object for the selection
         * @Param [R] The type of the [result] object for the selection
         *
         * @param target The target object for the selection.
         * @param result The result of the table operation.
         */
        public fun <T, R> defaultOnSelect(target: T, result: RollResult<R>): Unit = Unit

        /**
         * Default implementation for the getBaseDropRate function that returns 0.0.
         *
         * @param target The target object for which to get the base drop rate.
         * @returnAlways 0.0
         */
        public fun <T> defaultGetBaseDropRate(target: T): Double = 0.0

        /**
         * A singleton Rollable that always returns nothing.
         */
        public data object Empty: Rollable<Any?, Any?> {

            override fun roll(target: Any?, otherArgs: ArgMap): RollResult<Any?> {
                return RollResult.Companion.Nothing()
            }

            override fun getBaseDropRate(target: Any?): Double {
                return 0.0
            }
        }

        /**
         * Returns an empty Rollable with the specified type parameters.
         *
         * @return An empty Rollable instance.
         */
        public fun <T, R> Empty(): Rollable<T, R> {
            return Empty as Rollable<T, R>
        }
    }

    /**
     * A Rollable implementation that always returns a fixed result.
     *
     * @property result The fixed result to return.
     * @property onSelectFun The function to call when the result is selected.
     */
    public data class Single<T, R>(
        public val result: R,
        public val predicate: (T) -> Boolean = { true },
        public val onSelectFun: (T, RollResult<R>) -> Unit = ::defaultOnSelect
    ): Rollable<T, R> {

        override fun shouldRoll(target: T): Boolean = predicate(target)

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

    /**
     * A Rollable implementation that uses a function to generate a result.
     *  Particularly useful when returning a result with some stable data and some other non-stable data,
     *      such as dropping between 1 and 10 coins.
     *
     * @property resultSelector The function that generates the result.
     * @property onSelectFun The function to call when the result is selected.
     *
     * @sample
     * CoinRollable(
     *     resultSelector = { Item("Coin", Random.nextInt(1, 11)) }
     * )
     */
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

            if (!shouldRoll(target)){
                return RollResult.Nothing()
            }

            val result = RollResult.Single(resultSelector.invoke())
            onSelect(target, result)

            return result
        }
    }
}

/**
 * Builder class for creating Single OR SingleByFun Rollable instances.
 *
 * [Rollable.SingleByFun] takes precedence when both [result] AND [resultSelector] are defined
 *
 * @param T The type of the target object for which the roll is performed.
 * @param R The type of the result produced by the roll operation.
 */
public open class SingleRollableBuilder<T, R> {

    /**
     * The fixed result to return from the Rollable.
     */
    public var result: R? = null

    /**
     * The function that generates the result for the Rollable.
     */
    public var resultSelector: (() -> R)? = null

    /**
     * Predicate that decides whether or not to even roll this guy
     */
    public var shouldRollFunc: (T) -> Boolean = { true }

    /**
     * The function to call when a result is selected.
     */
    public var onSelectFun: (T, RollResult<R>) -> Unit = Rollable.Companion::defaultOnSelect

    /**
     * Sets the function to call when a result is selected.
     *
     * @param block The function to call.
     * @return This builder instance for method chaining.
     */
    public fun onSelect(block: (T, RollResult<R>) -> Unit): SingleRollableBuilder<T, R> = apply {
        onSelectFun = block
    }

    public fun shouldRoll(block: (T) -> Boolean): SingleRollableBuilder<T, R> = apply {
        shouldRollFunc = block
    }

    /**
     * Sets the fixed result to return from the Rollable.
     *
     * @param newResult The result to return.
     * @return This builder instance for method chaining.
     */
    public fun result(newResult: R): SingleRollableBuilder<T, R> = apply {
        result = newResult
    }

    /**
     * Sets the function that generates the result for the Rollable.
     *
     * @param resultFunc The function that generates the result.
     * @return This builder instance for method chaining.
     */
    public fun result(resultFunc: () -> R): SingleRollableBuilder<T, R> = apply {
        resultSelector = resultFunc
    }

    /**
     * Builds and returns a Rollable instance based on the current configuration.
     *
     * @return If a non-null [resultSelector] will result in a [Rollable.SingleByFun], otherwise a [Rollable.Single]
     * @throws IllegalStateException if both result and resultSelector are null.
     */
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

public fun <T, R> rollable(block: SingleRollableBuilder<T, R>.() -> Unit): Rollable<T, R> {
    val builder = SingleRollableBuilder<T, R>()
    builder.apply(block)
    return builder.build()
}
