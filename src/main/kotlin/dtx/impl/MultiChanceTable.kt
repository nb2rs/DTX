package dtx.impl

import dtx.core.ArgMap
import dtx.core.Rollable
import dtx.core.RollResult
import dtx.core.Rollable.Companion.defaultOnSelect
import dtx.core.Rollable.Companion.defaultGetBaseDropRate
import dtx.core.flattenToList
import dtx.table.AbstractTableBuilder
import dtx.table.Table
import dtx.table.Table.Companion.defaultRollModifier
import dtx.util.NoTransform
import kotlin.random.Random

public interface MultiChanceTable<T, R>: Table<T, R> {

    override val tableEntries: List<ChanceRollable<T, R>>

    public val maxRollChance: Double get() = 100.0 + Double.MIN_VALUE
}

public interface ChanceRollable<T, R>: Rollable<T, R> {

    public val chance: Double
    public val rollable: Rollable<T, R>

    public operator fun component1(): Double {
        return chance
    }

    public operator fun component2(): Rollable<T, R> {
        return rollable
    }

    public override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {
        return rollable.roll(target, otherArgs)
    }

    private data object Empty: ChanceRollable<Any?, Any?> {

        override val chance: Double = 0.0

        override val rollable: Rollable<Any?, Any?> = Rollable.Empty()
    }

    public companion object {

        public fun <T, R> Empty(): ChanceRollable<T, R> {
            return Empty as ChanceRollable<T, R>
        }
    }
}

internal data class ChanceRollableImpl<T, R>(
    override val chance: Double,
    override val rollable: Rollable<T, R>
): ChanceRollable<T, R>

public open class MultiChanceTableImpl<T, R>(
    public val tableName: String,
    entries: List<ChanceRollable<T, R>>,
    protected val rollModifierFunc: (Double) -> Double = ::defaultRollModifier,
    protected open val getBaseDropRateFunc: (T) -> Double = ::defaultGetBaseDropRate,
    public open val onSelectFunc: (T, RollResult<R>) -> Unit = ::defaultOnSelect
): MultiChanceTable<T, R> {

    public override val tableEntries: List<ChanceRollable<T, R>> = entries.map(NoTransform())

    public override fun onSelect(target: T, result: RollResult<R>) {
        return onSelectFunc(target, result)
    }


    public override fun rollModifier(percentage: Double): Double {
        return rollModifierFunc(percentage)
    }


    public override fun getBaseDropRate(target: T): Double {
        return getBaseDropRateFunc(target)
    }


    public override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

        if (tableEntries.isEmpty()) {
            return RollResult.Nothing()
        }

        val pickedEntries = mutableListOf<ChanceRollable<T, R>>()

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


    public override fun toString(): String = "MultiChanceTable[$tableName]"
}


public open class MultiChanceTableBuilder<T, R>: AbstractTableBuilder<T, R, MultiChanceTable<T, R>, ChanceRollable<T, R>, MultiChanceTableBuilder<T, R>>() {

    override val entries: MutableList<ChanceRollable<T, R>> = mutableListOf()

    public infix fun Percent.chance(rollable: Rollable<T, R>): MultiChanceTableBuilder<T, R> {

        addEntry(ChanceRollableImpl(value, rollable))

        return this@MultiChanceTableBuilder
    }

    public infix fun Percent.chance(entry: R): MultiChanceTableBuilder<T, R> {
        return chance(Rollable.Single(entry))
    }

    public infix fun Percent.chance(entryBlock: () -> R): MultiChanceTableBuilder<T, R> {
        return chance(Rollable.SingleByFun(entryBlock))
    }

    public infix fun Int.chance(rollable: Rollable<T, R>): MultiChanceTableBuilder<T, R> {
        return percent.chance(rollable = rollable)
    }

    public infix fun Int.chance(entry: R): MultiChanceTableBuilder<T, R> {
        return chance(Rollable.Single(entry))
    }

    public infix fun Int.chance(entryBlock: () -> R): MultiChanceTableBuilder<T, R> {
        return chance(Rollable.SingleByFun(entryBlock))
    }

    override fun build(): MultiChanceTable<T, R> {
        return MultiChanceTableImpl(
            tableName,
            entries,
            getRollModFunc,
            getDropRateFunc,
            onSelectFunc
        )
    }
}


public inline fun <T, R> multiChanceTable(
    tableName: String = "Unnamed Multi Chance Table",
    block: MultiChanceTableBuilder<T, R>.() -> Unit
): MultiChanceTable<T, R> {

    val builder = MultiChanceTableBuilder<T, R>()
    builder.apply { name(tableName) }
    builder.apply(block)

    return builder.build()
}
