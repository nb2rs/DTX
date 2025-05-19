package dtx.example

import dtx.core.ArgMap
import dtx.core.Rollable
import dtx.core.SingleRollableBuilder
import dtx.core.RollResult
import dtx.core.Rollable.Companion.defaultOnSelect
import dtx.core.Rollable.Companion.defaultGetBaseDropRate
import dtx.core.flattenToList
import dtx.table.Table
import dtx.table.Table.Companion.defaultRollModifier
import util.NoTransform
import kotlin.random.Random

public interface ChanceRollable<T, R>: Rollable<T, R> {

    public val chance: Double
    public val rollable: Rollable<T, R>

    public operator fun component1(): Double {
        return chance
    }

    public operator fun component2(): Rollable<T, R> {
        return rollable
    }

    public override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {
        return rollable.roll(target, otherArgs)
    }

    private data object Empty: ChanceRollable<Any?, Any?> {
        override val chance: Double = 0.0
        override val rollable: Rollable<Any?, Any?> = Rollable.Empty()
    }

    public companion object {
        public fun <T, R> Empty(): ChanceRollable<T, R> {
            return Empty as ChanceRollable<T, R>
        }
    }
}

internal data class ChanceRollableImpl<T, R>(
    override val chance: Double,
    override val rollable: Rollable<T, R>
): ChanceRollable<T, R>


/**
 * A [dtx.table.Table] implementation that gives each item a chance to be rolled independently.
 *
 * MultiChanceTable evaluates each item's chance independently, allowing multiple items
 * to be selected in a single roll. Items with a 100% chance are always selected.
 * The selection can be modified by a roll modifier that adjusts the effective chances.
 *
 * @param T The type of the target object for which the roll is performed.
 * @param R The type of the result produced by the roll operation.
 * @property tableName The name of the table, can be used for identification and debugging.
 * @property ignoreModifier Whether roll modifiers should be ignored for this table.
 */
public open class MultiChanceTable<T, R>(
    public val tableName: String,
    entries: List<ChanceRollable<T, R>>,
    override val ignoreModifier: Boolean = false,
    protected val rollModifierFunc: (Double) -> Double = ::defaultRollModifier,
    protected open val getBaseDropRateFunc: (T) -> Double = ::defaultGetBaseDropRate,
    public open val onSelectFunc: (T, RollResult<R>) -> Unit = ::defaultOnSelect
): Table<T, R> {

    init {
        require(entries.isNotEmpty()) {
            "table[$tableName] entries must not be empty"
        }
    }

    /**
     * The maximum possible roll value
     */
    protected val maxRollChance: Double = 100.0 + Double.MIN_VALUE

    /**
     * The collection of entries in this table.
     * Each entry is a Rollable that can be selected during a roll operation.
     */
    public override val tableEntries: List<ChanceRollable<T, R>> = entries.map(NoTransform())

    /**
     * Called when a [RollResult] is selected from this table.
     *
     * @param target The target object for which the selection was made.
     * @param result [RollResult].
     */
    public override fun onSelect(target: T, result: RollResult<R>) {
        return onSelectFunc(target, result)
    }

    /**
     * Calculates a roll modifier based on a percentage.
     *
     * @param percentage The percentage to use for the modifier calculation.
     * @return [Double]
     */
    public override fun rollModifier(percentage: Double): Double {
        return rollModifierFunc(percentage)
    }

    /**
     *
     */
    public override fun getBaseDropRate(target: T): Double {
        return getBaseDropRateFunc(target)
    }

    /**
     * Performs a roll by evaluating each item's chance independently.
     *
     * For each item, a random number between 0 and 100 is rolled. If the roll
     * multiplied by the modifier is greater than or equal to the item's chance,
     * the item is selected. Items with a 100% chance are always selected.
     * All selected items are combined into a single [RollResult.ListOf].
     *
     * @param target The target object for which to perform the roll.
     * @param otherArgs Additional arguments for the roll operation.
     * @return [RollResult.ListOf]
     */
    public override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

        val pickedEntries = mutableListOf<ChanceRollable<T, R>>()
        val modifier = rollModifier(getBaseDropRate(target))

        tableEntries.forEach { entry ->
            if (entry.chance == 100.0) {
                pickedEntries.add(entry)
                return@forEach
            }

            val roll = Random.nextDouble(0.0, maxRollChance)

            if (roll * modifier <= entry.chance) {
                pickedEntries.add(entry)
            }
        }

        val results = pickedEntries.map { it.roll(target, otherArgs) }.flattenToList()

        onSelect(target, results)

        return results
    }

    /**
     * Returns a string representation of this table.
     *
     * @return [String]
     */
    public override fun toString(): String = "MultiChanceTable[$tableName]"
}

/**
 * Builder class for creating [MultiChanceTable] instances.
 *
 * @param T The type of the target object for which the roll is performed.
 * @param R The type of the result produced by the roll operation.
 */
public class MultiChanceTableBuilder<T, R> {

    /**
     * The name of the table being built.
     */
    public var tableName: String = "Unnamed MultiChance Table"

    /**
     * Whether roll modifiers should be ignored for the table.
     */
    public var ignoreModifier: Boolean = false

    /**
     * The list of items to include in the table, each with its chance percentage.
     */
    private val entries = mutableListOf<ChanceRollable<T, R>>()

    /**
     * The function that determines the base drop rate for a target.
     */
    public var targetDropRate: (T) -> Double = { 1.0 }

    /**
     * The function to call when a result is selected from the table.
     */
    public var onSelect: (T, RollResult<R>) -> Unit = ::defaultOnSelect

    /**
     * The function that calculates the roll modifier.
     */
    public var rollModifier: (Double) -> Double = ::defaultRollModifier

    /**
     * Sets the name of the table.
     *
     * @param string The name to set.
     * @return This builder instance for method chaining.
     */
    public fun name(string: String): MultiChanceTableBuilder<T, R> = apply {
        tableName = string
    }

    /**
     * Sets the function to call when a result is selected from the table.
     *
     * @param block The function to call.
     * @return This builder instance for method chaining.
     */
    public fun onSelect(block: (T, RollResult<R>) -> Unit): MultiChanceTableBuilder<T, R> = apply {
        onSelect = block
    }

    /**
     * Sets the function that determines the base drop rate for a target.
     *
     * @param block The function to use.
     * @return This builder instance for method chaining.
     */
    public fun targetDropRate(block: (T) -> Double): MultiChanceTableBuilder<T, R> = apply {
        targetDropRate = block
    }

    /**
     * Sets the function that calculates the roll modifier.
     *
     * @param block The function to use.
     * @return This builder instance for method chaining.
     */
    public fun rollModifier(block: (Double) -> Double): MultiChanceTableBuilder<T, R> = apply {
        rollModifier = block
    }

    /**
     * Adds a [Rollable] to the table with a specified chance percentage.
     *
     * @param rollable The rollable to add.
     * @return This builder instance for method chaining.
     */
    public infix fun Percent.chance(rollable: Rollable<T, R>): MultiChanceTableBuilder<T, R> {
        entries.add(ChanceRollableImpl(this.value, rollable))
        return this@MultiChanceTableBuilder
    }

    /**
     * Adds a value to the table with a specified chance percentage.
     *
     * The value is wrapped in a [Rollable.Single].
     *
     * @param item The value to add.
     * @return This builder instance for method chaining.
     */
    public infix fun Percent.chance(item: R): MultiChanceTableBuilder<T, R> {
        return chance(Rollable.Single(item))
    }

    /**
     * Adds a rollable created using a [SingleRollableBuilder] to the table with a specified chance percentage.
     *
     * @param block The configuration block for the [SingleRollableBuilder].
     * @return This builder instance for method chaining.
     */
    public inline infix fun Percent.chance(block: SingleRollableBuilder<T, R>.() -> Unit): MultiChanceTableBuilder<T, R> {
        return chance(SingleRollableBuilder<T, R>().apply(block).build())
    }

    /**
     * Builds and returns a [MultiChanceTable] instance based on the current configuration.
     *
     * @return [MultiChanceTable]
     */
    public fun build(): MultiChanceTable<T, R> {
        return MultiChanceTable(tableName, entries, ignoreModifier, rollModifier, targetDropRate, onSelect)
    }
}

/**
 * Creates a [MultiChanceTable] using a builder.
 *
 * @param tableName The name of the table.
 * @param block The configuration block for the [MultiChanceTableBuilder].
 * @return [MultiChanceTable]
 */
public inline fun <T, R> multiChanceTable(tableName: String = "", block: MultiChanceTableBuilder<T, R>.() -> Unit): MultiChanceTable<T, R> {
    return MultiChanceTableBuilder<T, R>()
        .apply { name(tableName) }
        .apply(block)
        .build()
}
