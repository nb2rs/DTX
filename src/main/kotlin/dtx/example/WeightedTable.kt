package dtx.example

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.Rollable
import dtx.core.SingleRollableBuilder
import dtx.core.Rollable.Companion.defaultOnSelect
import dtx.core.Rollable.Companion.defaultGetBaseDropRate
import dtx.table.Table
import dtx.table.Table.Companion.defaultRollModifier
import util.NoTransform
import kotlin.random.Random

/**
 * A [dtx.table.Table] implementation that selects entries with probabilities proportional to their weights.
 *
 * WeightedTable randomly selects one of its values with a probability proportional to its weight
 * and returns the result of rolling that value. The selection can be modified by a roll modifier
 * that adjusts the effective weights of each entry.
 *
 * @param T The type of the target object for which the roll is performed.
 * @param R The type of the result produced by the roll operation.
 * @property tableIdentifier The name of the table, can be used for identification and debugging.
 * @property ignoreModifier Whether roll modifiers should be ignored for this table.
 */
public open class WeightedTable<T, R>(
    public val tableIdentifier: String,
    entries: List<WeightedRollable<T, R>>,
    override val ignoreModifier: Boolean = false,
    protected val rollModifierFunc: (Double) -> Double = ::defaultRollModifier,
    protected val getTargetDropRate: (T) -> Double = ::defaultGetBaseDropRate,
    protected open val onSelectFunc: (T, RollResult<R>) -> Unit = ::defaultOnSelect
): Table<T, R> {

    init {
        require(entries.isNotEmpty()) {
            "WeightedTable[$tableIdentifier] entries must not be empty"
        }

        require(entries.distinctBy { it.weight }.size == entries.size) {
            "WeightedTable[$tableIdentifier] entries must not have duplicate weights"
        }

    }

    /**
     * The sum of all weights in this table.
     */
    protected var weightSum: Double = entries.sumOf { it.weight }

    /**
     * The collection of entries in this table.
     * Each entry is a Rollable that can be selected during a roll operation.
     */
    public override val tableEntries: List<WeightedRollable<T, R>> = entries.map(NoTransform())

    /**
     * Returns the base drop rate for the given target.
     *
     * @param target The target object for which to get the base drop rate.
     * @return The base drop rate as determined by the getTargetDropRate function.
     */
    public override fun getBaseDropRate(target: T): Double {
        return getTargetDropRate(target)
    }

    /**
     * Calculates a roll modifier based on a percentage.
     *
     * If [ignoreModifier] is true, this method always returns 1.0.
     * Otherwise, it uses the rollModifierFunc to calculate the modifier.
     *
     * @param percentage The percentage to use for the modifier calculation.
     * @return The calculated modifier, or 1.0 if modifiers are ignored.
     */
    override fun rollModifier(percentage: Double): Double {

        if (ignoreModifier) {
            return 1.0
        }

        return rollModifierFunc(percentage)
    }

    /**
     * Called when a [RollResult] is selected from this table.
     *
     * @param target The target object for which the selection was made.
     * @param result The resulting [RollResult] of the selection that will eventually be returned by [roll]
     */
    public override fun onSelect(target: T, result: RollResult<R>): Unit {
        return onSelectFunc(target, result)
    }

    protected fun checkLowEntries(): Pair<Boolean, WeightedRollable<T, R>> {

        if (tableEntries.isEmpty()) {
            return true to WeightedRollable.Empty()
        }

        if (tableEntries.size == 1) {
            val singleResult = tableEntries.first()
            return true to singleResult
        }

        return false to WeightedRollable.Empty()
    }

    protected fun <E: WeightedRollable<T, R>> getWeightedEntry(
        rollMod: Double,
        rolledWeight: Double,
        usingEntries: Collection<E>
    ): E {
        var rolledWeight = rolledWeight

        usingEntries.asSequence()
            .map { entry -> (entry.weight * rollMod) to entry }
            .forEach { (weight, item) ->

                rolledWeight -= weight

                if (rolledWeight <= 0.0) {
                    return item
                }
            }

        return usingEntries.last()
    }

    /**
     * Performs a roll by randomly selecting one of the values with a probability
     * proportional to its weight, and rolling it.
     *
     * The selection is influenced by the roll modifier, which adjusts the
     * effective weights based on the base drop rate of the target.
     *
     * @param target The target object for which to perform the roll.
     * @param otherArgs Additional arguments for the roll operation.
     * @return [RollResult]
     */
    public override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

        val lowEntries = checkLowEntries()

        if (lowEntries.first) {
            val result = lowEntries.second.roll(target, otherArgs)
            onSelect(target, result)
            return result
        }

        val rollMod = rollModifier(getBaseDropRate(target))
        val rolledWeight = Random.nextDouble(0.0, weightSum)

        val pickedEntry = getWeightedEntry(rollMod, rolledWeight, tableEntries)
        val result = pickedEntry.roll(target, otherArgs)
        onSelect(target, result)

        return result
    }

    /**
     * Returns a string representation of this table.
     *
     * @return A string in the format "WeightedTable[tableIdentifier]".
     */
    public override fun toString(): String = "WeightedTable[$tableIdentifier]"
}

/**
 * Builder class for creating [WeightedTable]
 *
 * @param T The type of the target object for which the roll is performed.
 * @param R The type of the result produced by the roll operation.
 */
public open class WeightedTableBuilder<T, R> {

    /**
     * The name of the table being built.
     */
    public var tableName: String = "Unnamed Weighted Drop Table"

    /**
     * Whether roll modifiers should be ignored for the table.
     */
    public var ignoreModifier: Boolean = false

    /**
     * The function to call when a result is selected from the table.
     */
    public var onSelectFunc: (T, RollResult<R>) -> Unit = ::defaultOnSelect

    /**
     * The function that determines the base drop rate for a target.
     */
    public var targetDropRateFunc: (T) -> Double = { 1.0 }

    /**
     * The function that calculates the roll modifier.
     */
    public var rollModFunc: (Double) -> Double = ::defaultRollModifier

    /**
     * The list of weighted entries in the table.
     */
    private val entries: MutableList<WeightedRollable<T, R>> = mutableListOf()

    /**
     * Sets the function to call when a result is selected from the table.
     *
     * @param block The function to call.
     * @return This builder instance for method chaining.
     */
    public fun onSelect(block: (T, RollResult<R>) -> Unit): WeightedTableBuilder<T, R> {
        onSelectFunc = block
        return this
    }

    /**
     * Sets the function that calculates the roll modifier.
     *
     * @param block The function to use.
     * @return This builder instance for method chaining.
     */
    public fun rollmodifier(block: (Double) -> Double): WeightedTableBuilder<T, R> {
        rollModFunc = block
        return this
    }

    /**
     * Sets the function that determines the base drop rate for a target.
     *
     * @param block The function to use.
     * @return This builder instance for method chaining.
     */
    public fun targetDropRate(block: (T) -> Double): WeightedTableBuilder<T, R> {
        targetDropRateFunc = block
        return this
    }

    /**
     * Sets the name of the table.
     *
     * @param string The name to set.
     * @return This builder instance for method chaining.
     */
    public fun name(string: String): WeightedTableBuilder<T, R> {
        tableName = string
        return this
    }


    public fun ignoreModifier(ignore: Boolean): WeightedTableBuilder<T, R> {
        ignoreModifier = ignore
        return this
    }

    /**
     * Adds a weighted [Rollable] to the table.
     *
     * @param rollable The [Rollable] to add.
     * @return This builder instance for method chaining.
     */
    public infix fun Double.weight(rollable: Rollable<T, R>): WeightedTableBuilder<T, R> {
        entries.add(WeightedRollableImpl(this, rollable))
        return this@WeightedTableBuilder
    }

    /**
     * Adds a weighted value to the table.
     *
     * The value is wrapped in a [Rollable.Single].
     *
     * @param entry The value to add.
     * @return This builder instance for method chaining.
     */
    public inline infix fun Double.weight(entry: R): WeightedTableBuilder<T, R> {
        return weight(Rollable.Single(entry))
    }

    /**
     * Adds a weighted Rollable created using a [SingleRollableBuilder] to the table.
     *
     * @param build The configuration block for the [SingleRollableBuilder].
     * @return This builder instance for method chaining.
     */
    public inline infix fun Double.weight(build: SingleRollableBuilder<T, R>.() -> Unit): WeightedTableBuilder<T, R> {
        return weight(SingleRollableBuilder<T, R>().apply(build).build())
    }

    /**
     * Adds a weighted Rollable to the table.
     *
     * The integer weight is converted to a double.
     *
     * @param rollable The Rollable to add.
     * @return This builder instance for method chaining.
     */
    public inline infix fun Int.weight(rollable: Rollable<T, R>): WeightedTableBuilder<T, R>{
        return toDouble() weight rollable
    }

    /**
     * Adds a weighted value to the table.
     *
     * @param item The value to add.
     * @return This builder instance for method chaining.
     */
    public inline infix fun Int.weight(item: R): WeightedTableBuilder<T, R> {
        return weight(Rollable.Single(item))
    }

    /**
     * Adds a weighted [Rollable] created using a [SingleRollableBuilder] to the table.
     *
     * @param build The configuration block for the [SingleRollableBuilder].
     * @return This builder instance for method chaining.
     */
    public inline infix fun Int.weight(build: SingleRollableBuilder<T, R>.() -> Unit): WeightedTableBuilder<T, R> {
        return toDouble().weight(SingleRollableBuilder<T, R>().apply(build).build())
    }

    /**
     * Builds and returns a [WeightedTable] instance based on the current configuration.
     *
     * @return A [WeightedTable] instance.
     */
    public fun build(): WeightedTable<T, R> {
        return WeightedTable(tableName, entries, ignoreModifier, rollModFunc, targetDropRateFunc, onSelectFunc)
    }
}

/**
 * Creates a [WeightedTable] using a builder.
 *
 * @param tableName The name of the table.
 * @param block The configuration block for the [WeightedTableBuilder].
 * @return A [WeightedTable] instance.
 */
public fun <T, R> weightedTable(
    tableName: String = "",
    block: WeightedTableBuilder<T, R>.() -> Unit
): WeightedTable<T, R> {
    return WeightedTableBuilder<T, R>()
        .apply { name(tableName) }
        .apply(block)
        .build()
}
