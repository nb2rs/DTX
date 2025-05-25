package dtx.impl

import dtx.core.ArgMap
import dtx.core.Rollable
import dtx.core.SingleRollableBuilder
import dtx.core.RollResult
import dtx.core.Rollable.Companion.defaultOnSelect
import dtx.core.Rollable.Companion.defaultGetBaseDropRate
import dtx.core.flattenToList
import dtx.core.singleRollable
import dtx.table.Table.Companion.defaultRollModifier
import dtx.util.NoTransform
import kotlin.random.Random


public class MetaChanceRollable<T, R>(
    public override val rollable: Rollable<T, R>,
    public override val identifier: String,
    public val initialChance: Double,
    public val minChance: Double,
    public val maxChance: Double,
    public override val metaEntryFilters: MutableSet<MetaEntryFilter<T, R>> = mutableSetOf()
): MetaRollable<T, R>, ChanceRollable<T, R> {

    public override var chance: Double = initialChance.coerceIn(minChance, maxChance)
        public set (value) { field = value.coerceIn(minChance, maxChance) }

    override lateinit var parentTable: MetaMultiChanceTable<T, R>


    public fun increaseCurrentChanceBy(amount: Double) {
        chance = (chance + amount)
    }


    public fun decreaseCurrentChanceBy(amount: Double) {
        chance = (chance - amount)
    }

    override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {
        return super<MetaRollable>.roll(target, otherArgs)
    }
}


public class MetaChanceRollableBuilder<T, R> {


    public var identifier: String = ""


    public var rollable: Rollable<T, R> = Rollable.Empty()


    public var chance: Double = 1.0


    public val filters: MutableSet<MetaEntryFilter<T, R>> = mutableSetOf()


    public var minChance: Double = 0.0


    public var maxChance: Double = 100.0


    public fun chance(chance: Double): MetaChanceRollableBuilder<T, R> {

        this.chance = chance

        return this
    }


    public fun minChance(chance: Double): MetaChanceRollableBuilder<T, R> {

        minChance = chance

        return this
    }


    public fun maxChance(chance: Double): MetaChanceRollableBuilder<T, R> {

        maxChance = chance

        return this
    }


    public fun identifier(string: String): MetaChanceRollableBuilder<T, R> {

        identifier = string

        return this
    }


    public inline fun id(string: String): MetaChanceRollableBuilder<T, R> {
        return identifier(string)
    }


    public fun rollable(newRollable: Rollable<T, R>): MetaChanceRollableBuilder<T, R> {
        rollable = newRollable

        return this
    }


    public fun rollable(item: R): MetaChanceRollableBuilder<T, R> {
        return rollable(Rollable.Single(item))
    }


    public fun rollable(block: () -> R): MetaChanceRollableBuilder<T, R> {
        return rollable(Rollable.SingleByFun(block))
    }


    public fun buildRollable(block: SingleRollableBuilder<T, R>.() -> Unit): MetaChanceRollableBuilder<T, R> {
        return rollable(singleRollable(block))
    }


    public fun addFilter(filter: MetaEntryFilter<T, R>): MetaChanceRollableBuilder<T, R> {

        filters.add(filter)

        return this
    }


    public fun addFilter(block: MetaEntryFilterBuilder<T, R>.() -> Unit): MetaChanceRollableBuilder<T, R> {
        return addFilter(MetaEntryFilterBuilder<T, R>().apply(block).build())
    }


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


public class MetaMultiChanceTableBuilder<T, R> {

    public var tableName: String = "Unnamed Meta Multi Chance Table"


    public var ignoreModifier: Boolean = false


    public var onSelectFunc: (T, RollResult<R>) -> Unit = ::defaultOnSelect


    public var targetDropRateFunc: (T) -> Double = { 1.0 }


    public var rollModFunc: (Double) -> Double = ::defaultRollModifier


    private val entries: MutableList<MetaChanceRollable<T, R>> = mutableListOf()


    public fun onSelect(block: (T, RollResult<R>) -> Unit): MetaMultiChanceTableBuilder<T, R> {

        onSelectFunc = block

        return this
    }


    public fun rollModifier(block: (Double) -> Double): MetaMultiChanceTableBuilder<T, R> {

        rollModFunc = block

        return this
    }


    public fun targetDropRate(block: (T) -> Double): MetaMultiChanceTableBuilder<T, R> {

        targetDropRateFunc = block

        return this
    }


    public fun name(string: String): MetaMultiChanceTableBuilder<T, R> {

        tableName = string

        return this
    }


    public fun ignoreModifier(ignore: Boolean): MetaMultiChanceTableBuilder<T, R> {

        ignoreModifier = ignore

        return this
    }


    public infix fun Percent.chance(rollable: MetaChanceRollable<T, R>): MetaMultiChanceTableBuilder<T, R> {

        rollable.chance = value * 100.0
        entries.add(rollable)

        return this@MetaMultiChanceTableBuilder
    }


    public inline infix fun Percent.chance(
        builder: MetaChanceRollableBuilder<T, R>.() -> Unit
    ): MetaMultiChanceTableBuilder<T, R> {

        val builder = MetaChanceRollableBuilder<T, R>()
        builder.builder()

        return chance(builder.build())
    }


    public fun build(): MetaMultiChanceTable<T, R> {
        return MetaMultiChanceTable(tableName, entries, ignoreModifier, rollModFunc, targetDropRateFunc, onSelectFunc)
    }
}


public inline fun <T, R> metaMultiChanceTable(
    tableName: String = "Unnamed Meta Multi Chance Table",
    block: MetaMultiChanceTableBuilder<T, R>.() -> Unit
): MetaMultiChanceTable<T, R> {

    val builder = MetaMultiChanceTableBuilder<T, R>()
    builder.apply { name(tableName) }
    builder.apply(block)

    return builder.build()
}
