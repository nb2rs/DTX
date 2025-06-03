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
import dtx.util.isSortedBy

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


    public override lateinit var parentTable: MetaWeightedTable<T, R>


    public fun increaseCurrentWeightBy(amount: Double) {
        currentWeight = (currentWeight + amount).coerceIn(minimumWeight, maximumWeight)
    }


    public fun decreaseCurrentWeightBy(amount: Double) {
        currentWeight = currentWeight - amount.coerceIn(minimumWeight, maximumWeight)
    }

    override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {
        return super<MetaRollable>.roll(target, otherArgs)
    }
}


public fun <T, R> entryFilter(block: MetaEntryFilterBuilder<T, R>.() -> Unit): MetaEntryFilter<T, R> {
    return MetaEntryFilterBuilder<T, R>().apply(block).build()
}


public class MetaWeightedRollableBuilder<T, R>: AbstractMetaRollableBuilder<T, R, MetaWeightedRollable<T, R>, MetaWeightedRollableBuilder<T, R>>() {

    public var weight: Double by ::initialValue
    public var minWeight: Double by ::minValue
    public var maxWeight: Double by ::maxValue

    public fun weight(weight: Double): MetaWeightedRollableBuilder<T, R> {
        return value(weight)
    }

    public fun minWeight(weight: Double): MetaWeightedRollableBuilder<T, R> {
        return minimum(weight)
    }

    public fun maxWeight(weight: Double): MetaWeightedRollableBuilder<T, R> {
        return maximum(weight)
    }

    public override fun build(): MetaWeightedRollable<T, R> {
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
    shouldRollFunc: ShouldRoll<T> = ::defaultShouldRoll,
    rollModifierFunc: RollModifier<T> = ::defaultRollModifier,
    getTargetDropRate: BaseDroprate<T> = ::defaultGetBaseDropRate,
    onSelectFunc: OnSelect<T, R> = ::defaultOnSelect
): MetaTable<T, R>, WeightedTableImpl<T, R>(
    tableName, entries, shouldRollFunc,
    rollModifierFunc, getTargetDropRate, onSelectFunc
) {
    public override val tableEntries: MutableList<MetaWeightedRollable<T, R>> = entries
        .map(NoTransform())
        .sortedBy { it.currentWeight }
        .toMutableList()

    override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

        val result = super<WeightedTableImpl>.roll(target, otherArgs)

        reSortWeights()

        return result
    }

    private fun reSortWeights(): Unit {

        if (tableEntries.isSortedBy(MetaWeightedRollable<T, R>::currentWeight)) {
            return
        }

        tableEntries.sortBy(MetaWeightedRollable<T, R>::currentWeight)
        maxRoll = tableEntries.sumOf(MetaWeightedRollable<T, R>::currentWeight)
    }

    init {
        tableEntries.forEach { it.parentTable = this }
    }
}

public class MetaWeightedTableBuilder<T, R>: AbstractTableBuilder<T, R, MetaWeightedTable<T, R>, MetaWeightedRollable<T, R>, MetaWeightedTableBuilder<T, R>>() {

    override val entries: MutableList<MetaWeightedRollable<T, R>> = mutableListOf()

    public infix fun Double.weight(rollable: MetaWeightedRollable<T, R>): MetaWeightedTableBuilder<T, R> {
        return addEntry(rollable)
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

    public inline infix fun Int.weight(builder: MetaWeightedRollableBuilder<T, R>.() -> Unit): MetaWeightedTableBuilder<T, R> {
        return toDouble().weight(builder)
    }

    public override fun build(): MetaWeightedTable<T, R> {
        return MetaWeightedTable(tableName, entries, shouldRollFunc, getRollModFunc, getDropRateFunc, onSelectFunc)
    }
}

public inline fun <T, R> metaWeightedTable(
    tableName: String = "Unnamed Meta Weighted Table",
    block: MetaWeightedTableBuilder<T, R>.() -> Unit
): MetaWeightedTable<T, R> {

    val builder = MetaWeightedTableBuilder<T, R>()
    builder.apply { name(tableName) }
    builder.apply(block)

    return builder.build()
}
