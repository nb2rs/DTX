package dtx.impl

import dtx.core.ArgMap
import dtx.core.Rollable
import dtx.core.SingleRollableBuilder
import dtx.core.RollResult
import dtx.core.Rollable.Companion.defaultOnSelect
import dtx.core.Rollable.Companion.defaultGetBaseDropRate
import dtx.core.singleRollable
import dtx.table.AbstractTableBuilder
import dtx.table.Table.Companion.defaultRollModifier
import dtx.util.NoTransform


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

    override lateinit var parentTable: MetaMultiChanceTableImpl<T, R>


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


public class MetaMultiChanceTableImpl<T, R>(
    tableName: String,
    entries: List<MetaChanceRollable<T, R>>,
    rollModifierFunc: (Double) -> Double = ::defaultRollModifier,
    getBaseDropRateFunc: (T) -> Double = ::defaultGetBaseDropRate,
    onSelectFunc: (T, RollResult<R>) -> Unit = ::defaultOnSelect
): MetaTable<T, R>, MultiChanceTableImpl<T, R>(
    tableName, entries,
    rollModifierFunc, getBaseDropRateFunc, onSelectFunc
) {

    public override val tableEntries: MutableList<MetaChanceRollable<T, R>> = entries
        .map(NoTransform())
        .toMutableList()

    init {
        tableEntries.forEach { it.parentTable = this }
    }
}


public class MetaMultiChanceTableBuilder<T, R>: AbstractTableBuilder<T, R, MetaMultiChanceTableImpl<T, R>, MetaChanceRollable<T, R>, MetaMultiChanceTableBuilder<T, R>>() {

    override val entries: MutableList<MetaChanceRollable<T, R>> = mutableListOf()

    public infix fun Percent.chance(rollable: MetaChanceRollable<T, R>): MetaMultiChanceTableBuilder<T, R> {

        rollable.chance = value * 100.0
        return addEntry(rollable)
    }


    public inline infix fun Percent.chance(
        builder: MetaChanceRollableBuilder<T, R>.() -> Unit
    ): MetaMultiChanceTableBuilder<T, R> {

        val builder = MetaChanceRollableBuilder<T, R>()
        builder.builder()

        return chance(builder.build())
    }


    public override fun build(): MetaMultiChanceTableImpl<T, R> {
        return MetaMultiChanceTableImpl(tableName, entries, getRollModFunc, getDropRateFunc, onSelectFunc)
    }
}


public inline fun <T, R> metaMultiChanceTable(
    tableName: String = "Unnamed Meta Multi Chance Table",
    block: MetaMultiChanceTableBuilder<T, R>.() -> Unit
): MetaMultiChanceTableImpl<T, R> {

    val builder = MetaMultiChanceTableBuilder<T, R>()
    builder.apply { name(tableName) }
    builder.apply(block)

    return builder.build()
}
