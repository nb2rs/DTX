package dtx.impl

import dtx.core.ArgMap
import dtx.core.BaseDroprate
import dtx.core.OnSelect
import dtx.core.RollModifier
import dtx.core.Rollable
import dtx.core.RollResult
import dtx.core.Rollable.Companion.defaultOnSelect
import dtx.core.Rollable.Companion.defaultGetBaseDropRate
import dtx.core.ShouldRoll
import dtx.core.defaultShouldRoll
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

    override fun shouldRoll(target: T): Boolean {
        return rollable.shouldRoll(target)
    }

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


public class MetaChanceRollableBuilder<T, R>: AbstractMetaRollableBuilder<T, R, MetaChanceRollable<T, R>, MetaChanceRollableBuilder<T, R>>() {

    public var chance: Double by ::initialValue
    public var minChance: Double by ::minValue
    public var maxChance: Double by ::maxValue

    public fun chance(chance: Double): MetaChanceRollableBuilder<T, R> {
        return value(chance)
    }

    public fun minChance(chance: Double): MetaChanceRollableBuilder<T, R> {
        return minimum(chance)
    }

    public fun maxChance(chance: Double): MetaChanceRollableBuilder<T, R> {
        return maximum(chance)
    }

    public override fun build(): MetaChanceRollable<T, R> {
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
    shouldRollFunc: ShouldRoll<T> = ::defaultShouldRoll,
    rollModifierFunc: RollModifier<T> = ::defaultRollModifier,
    getBaseDropRateFunc: BaseDroprate<T> = ::defaultGetBaseDropRate,
    onSelectFunc: OnSelect<T, R> = ::defaultOnSelect
): MetaTable<T, R>, MultiChanceTableImpl<T, R>(
    tableName, entries, shouldRollFunc,
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
        return MetaMultiChanceTableImpl(tableName, entries, shouldRollFunc, getRollModFunc, getDropRateFunc, onSelectFunc)
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
