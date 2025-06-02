package dtx.impl

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.Rollable
import dtx.core.SingleRollableBuilder
import dtx.core.Rollable.Companion.defaultOnSelect
import dtx.core.Rollable.Companion.defaultGetBaseDropRate
import dtx.core.singleRollable
import dtx.table.AbstractTableBuilder
import dtx.table.Table
import dtx.table.Table.Companion.defaultRollModifier
import dtx.util.NoTransform
import kotlin.random.Random
import kotlin.sequences.forEach

public interface WeightedTable<T, R>: Table<T, R> {

    public override val tableEntries: List<WeightedRollable<T, R>>

    public val maxRoll: Double

    public fun <E: WeightedRollable<T, R>> getWeightedEntry(
        modifier: Double,
        rolled: Double,
        byEntries: Collection<E>,
        selector: (E) -> Double = WeightedRollable<T, R>::weight
    ): WeightedRollable<T, R> {

        var rolledWeight = rolled

        byEntries.asSequence()
            .map { entry -> (selector(entry) * modifier) to entry }
            .forEach { (weight, item) ->

                rolledWeight -= weight

                if (rolledWeight <= 0.0) {
                    return item
                }
            }

        return byEntries.last()
    }
}

public open class WeightedTableImpl<T, R>(
    public val tableIdentifier: String,
    entries: List<WeightedRollable<T, R>>,
    protected val rollModifierFunc: (Double) -> Double = ::defaultRollModifier,
    protected val getTargetDropRate: (T) -> Double = ::defaultGetBaseDropRate,
    protected open val onSelectFunc: (T, RollResult<R>) -> Unit = ::defaultOnSelect
): WeightedTable<T, R> {


    override var maxRoll: Double = entries.sumOf { it.weight }
    protected set

    public override val tableEntries: List<WeightedRollable<T, R>> = entries.map(NoTransform())

    public override fun getBaseDropRate(target: T): Double {
        return getTargetDropRate(target)
    }

    override fun rollModifier(percentage: Double): Double {
        return rollModifierFunc(percentage)
    }


    public override fun onSelect(target: T, result: RollResult<R>): Unit {
        return onSelectFunc(target, result)
    }

    public override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

        when (tableEntries.size) {

            0 -> {

                val nothing = RollResult.Nothing<R>()
                onSelect(target, nothing)

                return nothing
            }

            1 -> {

                val single = tableEntries.first().roll(target, otherArgs)
                onSelect(target, single)

                return single
            }
        }

        val rollMod = rollModifier(getBaseDropRate(target))
        val rolledWeight = Random.nextDouble(0.0, maxRoll)
        val pickedEntry = getWeightedEntry(rollMod, rolledWeight, tableEntries)
        val result = pickedEntry.roll(target, otherArgs)
        onSelect(target, result)

        return result
    }

    public override fun toString(): String = "WeightedTable[$tableIdentifier]"

    init {
        require(entries.distinctBy { it.weight }.size == entries.size) {
            "WeightedTable[$tableIdentifier] entries must not have duplicate weights"
        }

        require(entries.isNotEmpty()) {
            "WeightedTable[$tableIdentifier] entries must not be empty"
        }
    }
}


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
