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
import dtx.util.isSortedBy
import kotlin.random.Random

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


public class MetaWeightedRollableBuilder<T, R> {

    public var identifier: String = ""


    public var rollable: Rollable<T, R> = Rollable.Empty()


    public var weight: Double = 1.0


    public val filters: MutableSet<MetaEntryFilter<T, R>> = mutableSetOf()

    public var minWeight: Double = 0.0

    public var maxWeight: Double = 100.0


    public fun weight(newWeight: Double): MetaWeightedRollableBuilder<T, R> {

        weight = newWeight

        return this
    }


    public fun minWeight(weight: Double): MetaWeightedRollableBuilder<T, R> {

        minWeight = weight

        return this
    }


    public fun maxWeight(weight: Double): MetaWeightedRollableBuilder<T, R> {

        maxWeight = weight

        return this
    }


    public fun identifier(string: String): MetaWeightedRollableBuilder<T, R> {

        identifier = string

        return this
    }


    public fun id(string: String): MetaWeightedRollableBuilder<T, R> {
        return identifier(string)
    }


    public fun rollable(newRollable: Rollable<T, R>): MetaWeightedRollableBuilder<T, R> {

        rollable = newRollable

        return this
    }


    public fun rollable(item: R): MetaWeightedRollableBuilder<T, R> {
        return rollable(Rollable.Single(item))
    }


    public fun rollable(block: () -> R): MetaWeightedRollableBuilder<T, R> {
        return rollable(Rollable.SingleByFun(block))
    }



    public fun buildRollable(block: SingleRollableBuilder<T, R>.() -> Unit): MetaWeightedRollableBuilder<T, R> {
        return rollable(singleRollable(block))
    }


    public fun addFilter(filter: MetaEntryFilter<T, R>): MetaWeightedRollableBuilder<T, R> {

        filters.add(filter)

        return this
    }


    public fun addFilter(block: MetaEntryFilterBuilder<T, R>.() -> Unit): MetaWeightedRollableBuilder<T, R> {
        return addFilter(MetaEntryFilterBuilder<T, R>().apply(block).build())
    }


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
    rollModifierFunc: (Double) -> Double = ::defaultRollModifier,
    getTargetDropRate: (T) -> Double = ::defaultGetBaseDropRate,
    onSelectFunc: (T, RollResult<R>) -> Unit = ::defaultOnSelect
): MetaTable<T, R>, WeightedTableImpl<T, R>(
    tableName, entries,
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
        return MetaWeightedTable(tableName, entries, getRollModFunc, getDropRateFunc, onSelectFunc)
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
