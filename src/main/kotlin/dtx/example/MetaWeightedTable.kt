package dtx.example

import dtx.core.ArgMap
import dtx.core.Rollable
import dtx.core.SingleRollableBuilder
import dtx.core.RollResult
import dtx.core.Rollable.Companion.defaultOnSelect
import dtx.core.Rollable.Companion.defaultGetBaseDropRate
import dtx.table.Table.Companion.defaultRollModifier
import util.NoTransform
import util.isSortedBy
import kotlin.random.Random



/**
 * A [Rollable] entry in a [MetaWeightedTable] with a weight that can change.
 *
 * @param T The type of the target object for which the roll is performed.
 * @param R The type of the result produced by the roll operation.
 * @property rollable The underlying Rollable that produces results.
 * @property identifier A string identifier used for filtering.
 * @property initialWeight The initial weight of this entry.
 * @property minimumWeight The minimum weight this entry can have.
 * @property maximumWeight The maximum weight this entry can have.
 * @property currentWeight The current weight of this entry, which can change over time.
 * @property metaEntryFilters The filters that determine how this entry affects other entries.
 */
public class MetaWeightedRollable<T, R>(
    public override val rollable: Rollable<T, R>,
    public override val identifier: String,
    public val initialWeight: Double,
    public val minimumWeight: Double,
    public val maximumWeight: Double,
    public var currentWeight: Double = initialWeight.coerceIn(minimumWeight, maximumWeight),
    public override val metaEntryFilters: MutableSet<MetaEntryFilter<T, R>> = mutableSetOf()
): MetaRollable<T, R>, WeightedRollable<T, R> {

    public override val weight: Double by ::currentWeight

    /**
     * The parent table that contains this entry.
     * It is here for possible complex meta-shenanigans.
     */
    public override lateinit var parentTable: MetaWeightedTable<T, R>

    /**
     * Increases [currentWeight] of this entry by the specified amount.
     *
     * The weight is clamped to the range [[minimumWeight], [maximumWeight]].
     *
     * @param amount The amount to increase [currentWeight] by.
     */
    public fun increaseCurrentWeightBy(amount: Double) {
        currentWeight = (currentWeight + amount).coerceIn(minimumWeight, maximumWeight)
    }

    /**
     * Decreases [currentWeight] of this entry by the specified amount.
     *
     * The weight is clamped to the range [[minimumWeight], [maximumWeight]].
     *
     * @param amount The amount to decrease [currentWeight] by.
     */
    public fun decreaseCurrentWeightBy(amount: Double) {
        currentWeight = currentWeight - amount.coerceIn(minimumWeight, maximumWeight)
    }

    override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {
        return super<MetaRollable>.roll(target, otherArgs)
    }
}

/**
 * Creates a [MetaEntryFilter] using a builder.
 *
 * @param block The configuration block for the MetaEntryFilterBuilder.
 * @return [MetaEntryFilter]
 */
public fun <T, R> entryFilter(block: MetaEntryFilterBuilder<T, R>.() -> Unit): MetaEntryFilter<T, R> {
    return MetaEntryFilterBuilder<T, R>().apply(block).build()
}

/**
 * Builder class for creating [MetaWeightedRollable] instances.
 *
 * @param T The type of the target object for which the roll is performed.
 * @param R The type of the result produced by the roll operation.
 */
public class MetaWeightedRollableBuilder<T, R> {

    /**
     * The identifier for the entry being built.
     */
    public var identifier: String = ""

    /**
     * The rollable for the entry being built.
     */
    public var rollable: Rollable<T, R> = Rollable.Empty()

    /**
     * The initial weight for the entry being built.
     */
    public var weight: Double = 1.0

    /**
     * The filters for the entry being built.
     */
    public val filters: MutableSet<MetaEntryFilter<T, R>> = mutableSetOf()

    /**
     * The minimum weight for the entry being built.
     */
    public var minWeight: Double = 0.0

    /**
     * The maximum weight for the entry being built.
     */
    public var maxWeight: Double = 100.0

    /**
     * Sets the initial weight for the entry.
     *
     * @param weight The weight to set.
     * @return This builder instance for method chaining.
     */
    public fun weight(weight: Double): MetaWeightedRollableBuilder<T, R> = apply {
        this.weight = weight
    }

    /**
     * Sets the minimum weight for the entry.
     *
     * @param weight The minimum weight to set.
     * @return This builder instance for method chaining.
     */
    public fun minWeight(weight: Double): MetaWeightedRollableBuilder<T, R> = apply {
        minWeight = weight
    }

    /**
     * Sets the maximum weight for the entry.
     *
     * @param weight The maximum weight to set.
     * @return This builder instance for method chaining.
     */
    public fun maxWeight(weight: Double): MetaWeightedRollableBuilder<T, R> = apply {
        maxWeight = weight
    }

    /**
     * Sets the identifier for the entry.
     *
     * @param string The identifier to set.
     * @return This builder instance for method chaining.
     */
    public fun identifier(string: String): MetaWeightedRollableBuilder<T, R> = apply {
        identifier = string
    }

    /**
     * Sets the identifier for the entry (shorthand for identifier).
     *
     * @param string The identifier to set.
     * @return This builder instance for method chaining.
     */
    public inline fun id(string: String): MetaWeightedRollableBuilder<T, R> =
        identifier(string)

    /**
     * Sets the rollable for the entry.
     *
     * @param newRollable The rollable to set.
     * @return This builder instance for method chaining.
     */
    public fun rollable(newRollable: Rollable<T, R>): MetaWeightedRollableBuilder<T, R> = apply {
        rollable = newRollable
    }

    /**
     * Sets the [Rollable] for the entry to a [Rollable.Single] with the given item.
     *
     * @param item The item for the [Rollable.Single].
     * @return This builder instance for method chaining.
     */
    public fun rollable(item: R): MetaWeightedRollableBuilder<T, R> = apply {
        rollable(Rollable.Single(item))
    }

    /**
     * Sets the rollable for the entry to a [Rollable.SingleByFun] with the given function.
     *
     * @param block The function for the [Rollable.SingleByFun].
     * @return This builder instance for method chaining.
     */
    public fun rollable(block: () -> R): MetaWeightedRollableBuilder<T, R> =
        rollable(Rollable.SingleByFun(block))

    /**
     * Sets the rollable for the entry using a [SingleRollableBuilder].
     *
     * @param block The configuration block for the [SingleRollableBuilder].
     * @return This builder instance for method chaining.
     */
    public fun buildRollable(block: SingleRollableBuilder<T, R>.() -> Unit): MetaWeightedRollableBuilder<T, R> =
        rollable(SingleRollableBuilder<T, R>().apply(block).build())

    /**
     * Adds a filter to the entry.
     *
     * @param filter The filter to add.
     * @return This builder instance for method chaining.
     */
    public fun addFilter(filter: MetaEntryFilter<T, R>): MetaWeightedRollableBuilder<T, R> = apply {
        filters.add(filter)
    }

    /**
     * Adds a filter to the entry using a [MetaEntryFilterBuilder].
     *
     * @param block The configuration block for the [MetaEntryFilterBuilder].
     * @return This builder instance for method chaining.
     */
    public fun addFilter(block: MetaEntryFilterBuilder<T, R>.() -> Unit): MetaWeightedRollableBuilder<T, R> =
        addFilter(MetaEntryFilterBuilder<T, R>().apply(block).build())

    /**
     * Builds and returns a [MetaWeightedRollable] instance based on the current configuration.
     *
     * @return [MetaWeightedRollable]
     */
    public fun build(): MetaWeightedRollable<T, R> {
        return MetaWeightedRollable<T, R>(
            rollable = rollable,
            identifier = identifier,
            minimumWeight = minWeight,
            maximumWeight = maxWeight,
            initialWeight = weight,
            metaEntryFilters = filters
        )
    }
}

public class MetaWeightedTable<T, R>(
    tableName: String,
    entries: List<MetaWeightedRollable<T, R>>,
    ignoreModifier: Boolean = false,
    rollModifierFunc: (Double) -> Double = ::defaultRollModifier,
    getTargetDropRate: (T) -> Double = ::defaultGetBaseDropRate,
    onSelectFunc: (T, RollResult<R>) -> Unit = ::defaultOnSelect
): MetaTable<T, R>, WeightedTable<T, R>(
    tableName, entries, ignoreModifier,
    rollModifierFunc, getTargetDropRate, onSelectFunc
) {
    public override val tableEntries: MutableList<MetaWeightedRollable<T, R>> = entries
        .map(NoTransform())
        .sortedBy { it.currentWeight }
        .toMutableList()

    override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

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
        reSortWeights()

        return result
    }

    private fun reSortWeights(): Unit {
        if (tableEntries.isSortedBy(MetaWeightedRollable<T, R>::currentWeight)) {
            return
        }
        tableEntries.sortBy(MetaWeightedRollable<T, R>::currentWeight)
        weightSum = tableEntries.sumOf(MetaWeightedRollable<T, R>::currentWeight)
    }

    init {
        tableEntries.forEach { it.parentTable = this }
    }
}

public class MetaWeightedTableBuilder<T, R> {
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
    private val entries: MutableList<MetaWeightedRollable<T, R>> = mutableListOf()

    /**
     * Sets the function to call when a result is selected from the table.
     *
     * @param block The function to call.
     * @return This builder instance for method chaining.
     */
    public fun onSelect(block: (T, RollResult<R>) -> Unit): MetaWeightedTableBuilder<T, R> {
        onSelectFunc = block
        return this
    }

    /**
     * Sets the function that calculates the roll modifier.
     *
     * @param block The function to use.
     * @return This builder instance for method chaining.
     */
    public fun rollmodifier(block: (Double) -> Double): MetaWeightedTableBuilder<T, R> {
        rollModFunc = block
        return this
    }

    /**
     * Sets the function that determines the base drop rate for a target.
     *
     * @param block The function to use.
     * @return This builder instance for method chaining.
     */
    public fun targetDropRate(block: (T) -> Double): MetaWeightedTableBuilder<T, R> {
        targetDropRateFunc = block
        return this
    }

    /**
     * Sets the name of the table.
     *
     * @param string The name to set.
     * @return This builder instance for method chaining.
     */
    public fun name(string: String): MetaWeightedTableBuilder<T, R> {
        tableName = string
        return this
    }

    public fun ignoreModifier(ignore: Boolean): MetaWeightedTableBuilder<T, R> {
        ignoreModifier = ignore
        return this
    }

    public infix fun Double.weight(rollable: MetaWeightedRollable<T, R>): MetaWeightedTableBuilder<T, R> {
        entries.add(rollable)
        return this@MetaWeightedTableBuilder
    }

    public inline infix fun Double.weight(
        builder: MetaWeightedRollableBuilder<T, R>.() -> Unit
    ): MetaWeightedTableBuilder<T, R> {
        val builder = MetaWeightedRollableBuilder<T, R>()
        builder.builder()
        builder.apply { weight = this@weight }
        return weight(builder.build())
    }

    public inline infix fun Int.weight(rollable: MetaWeightedRollable<T, R>): MetaWeightedTableBuilder<T, R> {
        return toDouble().weight(rollable)
    }

    public inline infix fun Int.weight(
        builder: MetaWeightedRollableBuilder<T, R>.() -> Unit
    ): MetaWeightedTableBuilder<T, R> {
        return toDouble().weight(builder)
    }

    public fun build(): MetaWeightedTable<T, R> {
        return MetaWeightedTable(tableName, entries, ignoreModifier, rollModFunc, targetDropRateFunc, onSelectFunc)
    }
}

public inline fun <T, R> metaWeightedTable(builder: MetaWeightedTableBuilder<T, R>.() -> Unit): MetaWeightedTable<T, R> {
    val builder = MetaWeightedTableBuilder<T, R>()
    builder.builder()
    return builder.build()
}