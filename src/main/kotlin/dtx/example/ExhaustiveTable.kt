package dtx.example

import dtx.core.ArgMap
import dtx.core.Rollable
import dtx.core.SingleRollableBuilder
import dtx.core.RollResult
import dtx.core.Rollable.Companion.defaultOnSelect
import dtx.table.Table
import kotlin.properties.ReadWriteProperty
import kotlin.random.Random
import kotlin.reflect.KProperty

/**
 * An interface for rollable items that can be exhausted after a certain number of rolls.
 *
 * @param T The type of the target object for which the roll is performed.
 * @param R The type of the result produced by the roll operation.
 */
public interface ExhaustiveRollable<T, R>: Rollable<T, R> {
    /**
     * Called when this rollable is exhausted.
     *
     * @param target The target object for which the roll was performed.
     */
    public fun onExhaust(target: T): Unit { }

    /**
     * Checks if this rollable is exhausted.
     *
     * @return True if this rollable is exhausted, false otherwise.
     */
    public fun isExhausted(): Boolean

    /**
     * Resets the exhaustion state of this rollable.
     */
    public fun resetExhaustible(): Unit { }
}

/**
 * A data class representing an entry in an [ExhaustiveTable] that can be exhausted after a certain number of rolls.
 *
 * This class implements [ExhaustiveRollable] and wraps another [Rollable], adding exhaustion functionality.
 * After a specified number of rolls, the entry becomes exhausted and will no longer produce results.
 *
 * @param T The type of the target object for which the roll is performed.
 * @param R The type of the result produced by the roll operation.
 * @property entry The underlying [Rollable] that produces results.
 * @property totalRolls The total number of times this entry can be rolled before becoming exhausted.
 * @property onExhaustFunc A function called when this entry becomes exhausted.
 */
public data class ExhaustiveRollableEntry<T, R>(
    public val entry: Rollable<T, R>,
    public val totalRolls: Int,
    public val onExhaustFunc: ExhaustiveRollableEntry<T, R>.(T) -> Unit = { }
): ExhaustiveRollable<T, R> {

    /**
     * The number of rolls remaining before this entry becomes exhausted.
     */
    private var rollsRemaining: Int = totalRolls

    /**
     * Gets the number of rolls remaining before this entry becomes exhausted.
     *
     * @return [Int]
     */
    public fun getRemainingRolls(): Int {
        return rollsRemaining
    }

    /**
     * Called when this entry is exhausted.
     *
     * @param target The target object for which the roll was performed.
     */
    public override fun onExhaust(target: T): Unit {
        onExhaustFunc(target)
    }

    /**
     * Checks if this entry is exhausted.
     *
     * @return [Boolean]
     */
    public override fun isExhausted(): Boolean {
        return rollsRemaining <= 0
    }

    /**
     * Performs a roll using the underlying [Rollable].
     *
     * @param target The target object for which to perform the roll.
     * @param otherArgs Additional arguments for the roll operation.
     * @return [RollResult]
     */
    public override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

        if (isExhausted()) {
            return RollResult.Nothing()
        }

        rollsRemaining--
        val result = entry.roll(target, otherArgs)

        if (rollsRemaining == 0) {
            onExhaust(target)
        }

        return result
    }

    /**
     * Resets the exhaustion state of this entry.
     *
     * Sets the number of rolls remaining back to the initial total number of rolls.
     */
    public override fun resetExhaustible() {
        rollsRemaining = totalRolls
    }
}

/**
 * A property delegate that ensures a property's value is always positive.
 *
 * @property throwOnError Whether to throw an error when a negative value is assigned.
 */
internal class PositiveInt(
    initialValue: Int,
    val throwOnError: Boolean = false
): ReadWriteProperty<Any?, Int> {

    /**
     * The current value of the property.
     *
     * If the initial value is negative, it is either set to 1 or an error is thrown,
     * depending on the value of [throwOnError].
     */
    var intValue = if (initialValue < 0) {
        if (throwOnError) {
            error("Negative intValue for PositiveInt")
        } else {
            1
        }
    } else {
        initialValue
    }

    /**
     * Gets the value of the property.
     *
     * @param thisRef The object containing the property.
     * @param property The property being accessed.
     * @return [Int]
     */
    override fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        return intValue
    }

    /**
     * Sets the value of the property.
     *
     * @param thisRef The object containing the property.
     * @param property The property being modified.
     * @param value The new value to assign to the property.
     */
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {

        if (value < 0) {

            if (throwOnError) {
                error("SetValue - Negative intValue for PositiveInt")
            }

            intValue = 0
        }

        intValue = value
    }

}

/**
 * A builder extending [SingleRollableBuilder] for creating [ExhaustiveRollableEntry] instances with a single result.
 *
 * @param T The type of the target object for which the roll is performed.
 * @param R The type of the result produced by the roll operation.
 */
public class ExhaustiveSingleRollableBuilder<T, R>: SingleRollableBuilder<T, R>() {

    /**
     * The function to call when the rollable is exhausted.
     */
    public var onExhaust: ExhaustiveRollable<T, R>.(T) -> Unit = ExhaustiveRollable<T, R>::onExhaust

    /**
     * The total number of times the rollable can be rolled before becoming exhausted.
     */
    public var totalRolls: Int by PositiveInt(1, throwOnError = false)

    /**
     * Sets the total number of rolls before exhaustion.
     *
     * @param value The number of rolls.
     * @return This builder instance for method chaining.
     */
    public fun totalRolls(value: Int): ExhaustiveSingleRollableBuilder<T, R> = apply {
        totalRolls = value
    }

    /**
     * Sets the function to call when the rollable is exhausted.
     *
     * @param block The function to call.
     * @return This builder instance for method chaining.
     */
    public fun onExhaust(block: ExhaustiveRollable<T, R>.(T) -> Unit): ExhaustiveSingleRollableBuilder<T, R> = apply {
        onExhaust = block
    }

    /**
     * Builds and returns an [ExhaustiveRollableEntry] instance based on the current configuration.
     *
     * @return An [ExhaustiveRollableEntry] instance.
     * @throws IllegalStateException If the result is null.
     */
    public override fun build(): Rollable<T, R> {

        resultSelector?.let {
            return ExhaustiveRollableEntry<T, R>(Rollable.SingleByFun<T, R>(it), totalRolls, onExhaust)
        }

        result?.let {
            return ExhaustiveRollableEntry<T, R>(Rollable.Single<T, R>(it), totalRolls, onExhaust)
        }

        error("Cannot build ExhaustiveSingleRollable with null result and null resultSelector")
    }
}

/**
 * A [dtx.table.Table] implementation that randomly selects from a list of [ExhaustiveRollableEntry] items.
 *
 * Each item in the table can be rolled a limited number of times before becoming exhausted.
 * When an item is exhausted, it is no longer eligible for selection until the table is reset.
 *
 * @param T The type of the target object for which the roll is performed.
 * @param R The type of the result produced by the roll operation.
 * @property tableName The name of the table, can be used for identification and debugging.
 * @property onSelectFunc The function to call when a result is selected from the table.
 * @property onExhaustFunc The function to call when the table is exhausted.
 */
public open class ExhaustiveTable<T, R>(
    public val tableName: String,
    initialItems: List<ExhaustiveRollableEntry<T, R>>,
    private val onSelectFunc: (T, RollResult<R>) -> Unit = ::defaultOnSelect,
    private val onExhaustFunc: ExhaustiveTable<T, R>.() -> Unit = { }
): Table<T, R>, ExhaustiveRollable<T, R> {

    /**
     * Whether roll modifiers should be ignored for this table.
     */
    override val ignoreModifier: Boolean = true

    /**
     * The collection of tableEntries in this table.
     * Each entry is a Rollable that can be selected during a roll operation.
     */
    override val tableEntries: List<ExhaustiveRollableEntry<T, R>> = initialItems.map { it.copy() }

    /**
     * Called when a [RollResult] is selected from this table.
     *
     * @param target The target object for which the selection was made.
     * @param result The resulting [RollResult] of the selection.
     */
    override fun onSelect(target: T, result: RollResult<R>) {
        return onSelectFunc(target, result)
    }

    /**
     * Resets all tableEntries in this table to their initial state.
     */
    public fun reset(): Unit {
        tableEntries.forEach(ExhaustiveRollableEntry<T, R>::resetExhaustible)
    }

    /**
     * Performs a roll by randomly selecting one of the non-exhausted items and rolling it.
     *
     * @param target The target object for which to perform the roll.
     * @param otherArgs Additional arguments for the roll operation.
     * @return [RollResult]
     */
    public override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

        val rollableItems = tableEntries
            .filter { it.getRemainingRolls() > 0 }

        val rolled = rollableItems.random()
        val result = rolled.roll(target, otherArgs)
        onSelect(target, result)

        return result
    }

    /**
     * Called when this table is exhausted.
     *
     * @param target The target object for which the roll was performed.
     */
    public override fun onExhaust(target: T) {
        onExhaustFunc()
    }

    /**
     * Checks if this table is exhausted.
     *
     * A table is exhausted when all of its items are exhausted.
     *
     * @return [Boolean]
     */
    public override fun isExhausted(): Boolean {
        return tableEntries.all { it.isExhausted() }
    }

    /**
     * Resets the exhaustion state of this table.
     *
     * Resets all items in this table to their initial state.
     */
    public override fun resetExhaustible() {
        tableEntries.forEach { it.resetExhaustible() }
    }
}

/**
 * Builder class for creating [ExhaustiveTable] instances.
 *
 * @param T The type of the target object for which the roll is performed.
 * @param R The type of the result produced by the roll operation.
 */
public open class ExhaustiveTableBuilder<T, R> {

    /**
     * The name of the table being built.
     */
    public var tableName: String = "Unnamed Exhaustive Table"

    /**
     * The list of items to include in the table.
     */
    protected val items: MutableList<ExhaustiveRollableEntry<T, R>> = mutableListOf<ExhaustiveRollableEntry<T, R>>()

    /**
     * The function to call when a result is selected from the table.
     */
    public var onSelect: (T, RollResult<R>) -> Unit = ::defaultOnSelect

    /**
     * The function to call when the table is exhausted.
     */
    public var onExhaust: ExhaustiveTable<T, R>.() -> Unit = { }

    /**
     * Sets the function to call when a result is selected from the table.
     *
     * @param block The function to call.
     * @return This builder instance for method chaining.
     */
    public fun onSelect(block: (T, RollResult<R>) -> Unit): ExhaustiveTableBuilder<T, R> = apply {
        onSelect = block
    }

    /**
     * Sets the function to call when the table is exhausted.
     *
     * @param block The function to call.
     * @return This builder instance for method chaining.
     */
    public fun onExhaust(block: ExhaustiveTable<T, R>.() -> Unit): ExhaustiveTableBuilder<T, R> = apply {
        onExhaust = block
    }

    /**
     * Sets the name of the table.
     *
     * @param string The name to set.
     * @return This builder instance for method chaining.
     */
    public fun name(string: String): ExhaustiveTableBuilder<T, R> = apply {
        tableName = string
    }

    /**
     * Adds an [ExhaustiveRollableEntry] to the table with a specified number of rolls.
     *
     * @param entry The entry to add.
     * @return This builder instance for method chaining.
     */
    public infix fun Int.rolls(entry: ExhaustiveRollableEntry<T, R>): ExhaustiveTableBuilder<T, R> {
        items.add(entry.copy(totalRolls = this))
        return this@ExhaustiveTableBuilder
    }

    /**
     * Adds a [Rollable] to the table with a specified number of rolls.
     *
     * The rollable is wrapped in an [ExhaustiveRollableEntry].
     *
     * @param entry The rollable to add.
     * @return This builder instance for method chaining.
     */
    public infix fun Int.rolls(entry: Rollable<T, R>): ExhaustiveTableBuilder<T, R> {
        return rolls(ExhaustiveRollableEntry(entry, this))
    }

    /**
     * Adds a value to the table with a specified number of rolls.
     *
     * The value is wrapped in a [Rollable.Single] and then in an [ExhaustiveRollableEntry].
     *
     * @param item The value to add.
     * @return This builder instance for method chaining.
     */
    public infix fun Int.rolls(item: R): ExhaustiveTableBuilder<T, R> {
        return rolls(Rollable.Single(item))
    }

    /**
     * Adds a rollable created using an [ExhaustiveSingleRollableBuilder] to the table with a specified number of rolls.
     *
     * @param block The configuration block for the [ExhaustiveSingleRollableBuilder].
     * @return This builder instance for method chaining.
     */
    public inline infix fun Int.rolls(block: ExhaustiveSingleRollableBuilder<T, R>.() -> Unit): ExhaustiveTableBuilder<T, R> {
        return rolls(ExhaustiveSingleRollableBuilder<T, R>().apply(block).build())
    }

    /**
     * Builds and returns an [ExhaustiveTable] instance based on the current configuration.
     *
     * @return An [ExhaustiveTable] instance.
     */
    public open fun build(): ExhaustiveTable<T, R> {
        return ExhaustiveTable(tableName, items, onSelect, onExhaust)
    }
}

/**
 * Creates an [ExhaustiveTable] using a builder.
 *
 * @param tableName The name of the table.
 * @param block The builder lambda for the [ExhaustiveTableBuilder].
 * @return [ExhaustiveTable]
 * */
public inline fun <T, R> uniformExhaustiveTable(
    tableName: String = "Unnamed Exhaustive Table",
    block: ExhaustiveTableBuilder<T, R>.() -> Unit
): ExhaustiveTable<T, R> {
    return ExhaustiveTableBuilder<T, R>()
        .apply { name(tableName) }
        .apply(block).build()
}

/**
 * A specialized [ExhaustiveTable] that selects tableEntries with probabilities proportional to their remaining rolls.
 *
 * Unlike the standard [ExhaustiveTable] which selects tableEntries uniformly, this table gives higher
 * probability to tableEntries with more remaining rolls. Theoretically this is a self-balancing table.
 *
 * @param T The type of the target object for which the roll is performed.
 * @param R The type of the result produced by the roll operation.
 * @property tableName The name of the table, can be used for identification and debugging.
 */
public class WeightedExhaustiveTable<T, R>(
    tableName: String = "Unnamed Exhaustive Table",
    initialItems: List<ExhaustiveRollableEntry<T, R>>,
    onSelectFunc: (T, RollResult<R>) -> Unit = ::defaultOnSelect,
): ExhaustiveTable<T, R>(tableName, initialItems, onSelectFunc) {

    /**
     * Performs a roll by selecting one of the non-exhausted items with a probability
     * proportional to its remaining rolls, and rolling it.
     *
     * @param target The target object for which to perform the roll.
     * @param otherArgs Additional arguments for the roll operation.
     * @return [RollResult]
     */
    public override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

        val rollableItems = tableEntries
            .filter { it.getRemainingRolls() > 0 }

        val weightedItems = rollableItems
            .map { it.getRemainingRolls().toDouble() to it }
            .sortedBy { it.first }

        var rolledWeight = Random.nextDouble(0.0, weightedItems.sumOf { it.first })

        weightedItems.forEach { (weight, item) ->

            rolledWeight -= weight

            if (rolledWeight <= 0.0) {

                val result = item.roll(target, otherArgs)
                onSelect(target, result)

                return result
            }
        }

        val lastItem = weightedItems.last()
        val result = lastItem.second.roll(target, otherArgs)
        onSelect(target, result)

        return result
    }
}

/**
 * Builder class for creating [WeightedExhaustiveTable] instances.
 *
 * This class extends [ExhaustiveTableBuilder] and overrides the [build] method
 * to return a [WeightedExhaustiveTable] instead of a standard [ExhaustiveTable].
 *
 * @param T The type of the target object for which the roll is performed.
 * @param R The type of the result produced by the roll operation.
 */
public class WeightedExhaustiveTableBuilder<T, R>: ExhaustiveTableBuilder<T, R>() {
    /**
     * Builds and returns a [WeightedExhaustiveTable] instance based on the current configuration.
     *
     * @return A [WeightedExhaustiveTable] instance.
     */
    override fun build(): WeightedExhaustiveTable<T, R> {
        return WeightedExhaustiveTable<T, R>(tableName, items, onSelect)
    }
}

/**
 * Creates a [WeightedExhaustiveTable] using a builder.
 *
 * @param tableName The name of the table.
 * @param block The builder lambda for the [WeightedTableBuilder].
 * @return [WeightedExhaustiveTable]
 * */
public inline fun <T, R> weightedExhaustiveTable(
    tableName: String = "Unnamed Weighted Exhaustive Table",
    block: ExhaustiveTableBuilder<T, R>.() -> Unit
): WeightedExhaustiveTable<T, R> {
    return WeightedExhaustiveTableBuilder<T, R>()
        .apply { name(tableName) }
        .apply(block).build()
}