package dtx.example

import dtx.core.ArgMap
import dtx.core.Rollable
import dtx.core.SingleRollableBuilder
import dtx.core.RollResult
import dtx.core.Rollable.Companion.defaultOnSelect
import dtx.core.Rollable.Companion.defaultGetBaseDropRate
import dtx.core.flattenToList
import dtx.table.Table.Companion.defaultRollModifier
import util.NoTransform
import kotlin.random.Random

/**
 * A [MetaWeightedRollable] entry in a [MetaMultiChanceTable] with a chance that can change.
 *
 * @param T The type of the target object for which the roll is performed.
 * @param R The type of the result produced by the roll operation.
 * @property rollable The wrapped [Rollable] that produces results.
 * @property identifier A string identifier used for filtering.
 * @property initialChance The initial chance of this entry.
 * @property minChance The minimum chance this entry can have.
 * @property maxChance The maximum chance this entry can have.
 * @property chance The current chance of this entry, which can change over time.
 * @property metaEntryFilters The filters that determine how this entry affects other entries.
 */
public class MetaChanceRollable<T, R>(
    public override val rollable: Rollable<T, R>,
    public override val identifier: String,
    public val initialChance: Double,
    public val minChance: Double,
    public val maxChance: Double,
    public override val metaEntryFilters: MutableSet<MetaEntryFilter<T, R>> = mutableSetOf()
): MetaRollable<T, R>, ChanceRollable<T, R> {
    /**
     * The current chance of this entry, which can change over time.
     */
    public override var chance: Double = initialChance.coerceIn(minChance, maxChance)
        public set (value) { field = value.coerceIn(minChance, maxChance) }

    override lateinit var parentTable: MetaMultiChanceTable<T, R>

    /**
     * Increases [chance] of this entry by the specified amount.
     *
     * The chance is clamped to the range [[minChance], [maxChance]].
     *
     * @param amount The amount to increase [chance] by.
     */
    public fun increaseCurrentChanceBy(amount: Double) {
        chance = (chance + amount)
    }

    /**
     * Decreases [chance] of this entry by the specified amount.
     *
     * The chance is clamped to the range [[minChance], [maxChance]].
     *
     * @param amount The amount to decrease [chance] by.
     */
    public fun decreaseCurrentChanceBy(amount: Double) {
        chance = (chance - amount)
    }

    override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {
        return super<MetaRollable>.roll(target, otherArgs)
    }
}

/**
 * Builder class for creating [MetaChanceRollable] instances.
 *
 * @param T The type of the target object for which the roll is performed.
 * @param R The type of the result produced by the roll operation.
 */
public class MetaChanceRollableBuilder<T, R> {

    /**
     * The identifier for the entry being built.
     */
    public var identifier: String = ""

    /**
     * The rollable for the entry being built.
     */
    public var rollable: Rollable<T, R> = Rollable.Empty()

    /**
     * The initial chance for the entry being built.
     */
    public var chance: Double = 1.0

    /**
     * The filters for the entry being built.
     */
    public val filters: MutableSet<MetaEntryFilter<T, R>> = mutableSetOf()

    /**
     * The minimum chance for the entry being built.
     */
    public var minChance: Double = 0.0

    /**
     * The maximum chance for the entry being built.
     */
    public var maxChance: Double = 100.0

    /**
     * Sets the initial chance for the entry.
     *
     * @param chance The chance to set.
     * @return This builder instance for method chaining.
     */
    public fun chance(chance: Double): MetaChanceRollableBuilder<T, R> = apply {
        this.chance = chance
    }

    /**
     * Sets the minimum chance for the entry.
     *
     * @param chance The minimum chance to set.
     * @return This builder instance for method chaining.
     */
    public fun minChance(chance: Double): MetaChanceRollableBuilder<T, R> = apply {
        minChance = chance
    }

    /**
     * Sets the maximum chance for the entry.
     *
     * @param chance The maximum chance to set.
     * @return This builder instance for method chaining.
     */
    public fun maxChance(chance: Double): MetaChanceRollableBuilder<T, R> = apply {
        maxChance = chance
    }

    /**
     * Sets the identifier for the entry.
     *
     * @param string The identifier to set.
     * @return This builder instance for method chaining.
     */
    public fun identifier(string: String): MetaChanceRollableBuilder<T, R> = apply {
        identifier = string
    }

    /**
     * Sets the identifier for the entry (shorthand for identifier).
     *
     * @param string The identifier to set.
     * @return This builder instance for method chaining.
     */
    public inline fun id(string: String): MetaChanceRollableBuilder<T, R> =
        identifier(string)

    /**
     * Sets the rollable for the entry.
     *
     * @param newRollable The rollable to set.
     * @return This builder instance for method chaining.
     */
    public fun rollable(newRollable: Rollable<T, R>): MetaChanceRollableBuilder<T, R> = apply {
        rollable = newRollable
    }

    /**
     * Sets the [Rollable] for the entry to a [Rollable.Single] with the given item.
     *
     * @param item The item for the [Rollable.Single].
     * @return This builder instance for method chaining.
     */
    public fun rollable(item: R): MetaChanceRollableBuilder<T, R> = apply {
        rollable(Rollable.Single(item))
    }

    /**
     * Sets the rollable for the entry to a [Rollable.SingleByFun] with the given function.
     *
     * @param block The function for the [Rollable.SingleByFun].
     * @return This builder instance for method chaining.
     */
    public fun rollable(block: () -> R): MetaChanceRollableBuilder<T, R> =
        rollable(Rollable.SingleByFun(block))

    /**
     * Sets the rollable for the entry using a [SingleRollableBuilder].
     *
     * @param block The configuration block for the [SingleRollableBuilder].
     * @return This builder instance for method chaining.
     */
    public fun buildRollable(block: SingleRollableBuilder<T, R>.() -> Unit): MetaChanceRollableBuilder<T, R> =
        rollable(SingleRollableBuilder<T, R>().apply(block).build())

    /**
     * Adds a filter to the entry.
     *
     * @param filter The filter to add.
     * @return This builder instance for method chaining.
     */
    public fun addFilter(filter: MetaEntryFilter<T, R>): MetaChanceRollableBuilder<T, R> = apply {
        filters.add(filter)
    }

    /**
     * Adds a filter to the entry using a [MetaEntryFilterBuilder].
     *
     * @param block The configuration block for the [MetaEntryFilterBuilder].
     * @return This builder instance for method chaining.
     */
    public fun addFilter(block: MetaEntryFilterBuilder<T, R>.() -> Unit): MetaChanceRollableBuilder<T, R> =
        addFilter(MetaEntryFilterBuilder<T, R>().apply(block).build())

    /**
     * Builds and returns a [MetaChanceRollable] instance based on the current configuration.
     *
     * @return [MetaChanceRollable]
     */
    public fun build(): MetaChanceRollable<T, R> {
        return MetaChanceRollable<T, R>(
            rollable = rollable,
            identifier = identifier,
            minChance = minChance,
            maxChance = maxChance,
            initialChance = chance,
            metaEntryFilters = filters
        )
    }
}

/**
 * A [MultiChanceTable] implementation that allows entries to have meta-information and dynamic chances.
 *
 * @param T The type of the target object for which the roll is performed.
 * @param R The type of the result produced by the roll operation.
 * @property tableName The name of the table, can be used for identification and debugging.
 * @property ignoreModifier Whether roll modifiers should be ignored for this table.
 */
public class MetaMultiChanceTable<T, R>(
    tableName: String,
    entries: List<MetaChanceRollable<T, R>>,
    ignoreModifier: Boolean = false,
    rollModifierFunc: (Double) -> Double = ::defaultRollModifier,
    getBaseDropRateFunc: (T) -> Double = ::defaultGetBaseDropRate,
    onSelectFunc: (T, RollResult<R>) -> Unit = ::defaultOnSelect
): MetaTable<T, R>, MultiChanceTable<T, R>(
    tableName, entries, ignoreModifier,
    rollModifierFunc, getBaseDropRateFunc, onSelectFunc
) {
    public override val tableEntries: MutableList<MetaChanceRollable<T, R>> = entries
        .map(NoTransform())
        .toMutableList()

    override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

        val pickedEntries = mutableListOf<MetaChanceRollable<T, R>>()
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

    init {
        tableEntries.forEach { it.parentTable = this }
    }
}

/**
 * Builder class for creating [MetaMultiChanceTable] instances.
 *
 * @param T The type of the target object for which the roll is performed.
 * @param R The type of the result produced by the roll operation.
 */
public class MetaMultiChanceTableBuilder<T, R> {
    /**
     * The name of the table being built.
     */
    public var tableName: String = "Unnamed Meta Multi Chance Table"

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
     * The list of entries in the table.
     */
    private val entries: MutableList<MetaChanceRollable<T, R>> = mutableListOf()

    /**
     * Sets the function to call when a result is selected from the table.
     *
     * @param block The function to call.
     * @return This builder instance for method chaining.
     */
    public fun onSelect(block: (T, RollResult<R>) -> Unit): MetaMultiChanceTableBuilder<T, R> {
        onSelectFunc = block
        return this
    }

    /**
     * Sets the function that calculates the roll modifier.
     *
     * @param block The function to use.
     * @return This builder instance for method chaining.
     */
    public fun rollModifier(block: (Double) -> Double): MetaMultiChanceTableBuilder<T, R> {
        rollModFunc = block
        return this
    }

    /**
     * Sets the function that determines the base drop rate for a target.
     *
     * @param block The function to use.
     * @return This builder instance for method chaining.
     */
    public fun targetDropRate(block: (T) -> Double): MetaMultiChanceTableBuilder<T, R> {
        targetDropRateFunc = block
        return this
    }

    /**
     * Sets the name of the table.
     *
     * @param string The name to set.
     * @return This builder instance for method chaining.
     */
    public fun name(string: String): MetaMultiChanceTableBuilder<T, R> {
        tableName = string
        return this
    }

    /**
     * Sets whether roll modifiers should be ignored for the table.
     *
     * @param ignore Whether to ignore roll modifiers.
     * @return This builder instance for method chaining.
     */
    public fun ignoreModifier(ignore: Boolean): MetaMultiChanceTableBuilder<T, R> {
        ignoreModifier = ignore
        return this
    }

    /**
     * Adds a [MetaChanceRollable] to the table with a specified chance percentage.
     *
     * @param rollable The rollable to add.
     * @return This builder instance for method chaining.
     */
    public infix fun Percent.chance(rollable: MetaChanceRollable<T, R>): MetaMultiChanceTableBuilder<T, R> {
        rollable.chance = value * 100.0
        entries.add(rollable)
        return this@MetaMultiChanceTableBuilder
    }

    /**
     * Adds a rollable created using a [MetaChanceRollableBuilder] to the table with a specified chance percentage.
     *
     * @param builder The configuration block for the [MetaChanceRollableBuilder].
     * @return This builder instance for method chaining.
     */
    public inline infix fun Percent.chance(
        builder: MetaChanceRollableBuilder<T, R>.() -> Unit
    ): MetaMultiChanceTableBuilder<T, R> {
        val builder = MetaChanceRollableBuilder<T, R>()
        builder.builder()
        return chance(builder.build())
    }

    /**
     * Builds and returns a [MetaMultiChanceTable] instance based on the current configuration.
     *
     * @return [MetaMultiChanceTable]
     */
    public fun build(): MetaMultiChanceTable<T, R> {
        return MetaMultiChanceTable(tableName, entries, ignoreModifier, rollModFunc, targetDropRateFunc, onSelectFunc)
    }
}

/**
 * Creates a [MetaMultiChanceTable] using a builder.
 *
 * @param builder The configuration block for the [MetaMultiChanceTableBuilder].
 * @return [MetaMultiChanceTable]
 */
public inline fun <T, R> metaMultiChanceTable(builder: MetaMultiChanceTableBuilder<T, R>.() -> Unit): MetaMultiChanceTable<T, R> {
    val builder = MetaMultiChanceTableBuilder<T, R>()
    builder.builder()
    return builder.build()
}
