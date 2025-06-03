package dtx.impl

import dtx.core.Rollable
import dtx.core.SingleRollableBuilder
import dtx.core.singleRollable
import dtx.table.AbstractTableBuilder

public open class WeightedTableBuilder<T, R>: AbstractTableBuilder<T, R, WeightedTable<T, R>, WeightedRollable<T, R>, WeightedTableBuilder<T, R>>() {

    override val entries: MutableList<WeightedRollable<T, R>> = mutableListOf()

    public infix fun Double.weight(rollable: Rollable<T, R>): WeightedTableBuilder<T, R> {
        return addEntry(WeightedRollableImpl(this, rollable))
    }

    public inline infix fun Double.weight(entry: R): WeightedTableBuilder<T, R> {
        return weight(Rollable.Single(entry))
    }

    public infix fun Double.weight(block: SingleRollableBuilder<T, R>.() -> Unit): WeightedTableBuilder<T, R> {
        return weight(singleRollable(block))
    }

    public inline infix fun Int.weight(rollable: Rollable<T, R>): WeightedTableBuilder<T, R>{
        return toDouble() weight rollable
    }

    public inline infix fun Int.weight(item: R): WeightedTableBuilder<T, R> {
        return weight(Rollable.Single(item))
    }

    public infix fun Int.weight(block: SingleRollableBuilder<T, R>.() -> Unit): WeightedTableBuilder<T, R> {
        return toDouble().weight(block)
    }

    override fun build(): WeightedTable<T, R> {
        return WeightedTableImpl(
            tableIdentifier = tableName,
            entries = entries,
            shouldRollFunc = shouldRollFunc,
            rollModifierFunc = getRollModFunc,
            getTargetDropRate = getDropRateFunc,
            onSelectFunc = onSelectFunc
        )
    }
}

public fun <T, R> weightedTable(
    tableName: String = "Unnamed Weighted Table",
    block: WeightedTableBuilder<T, R>.() -> Unit
): WeightedTable<T, R> {

    val builder = WeightedTableBuilder<T, R>()
    builder.apply { name(tableName) }
    builder.apply(block)

    return builder.build()
}