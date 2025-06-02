package dtx.impl

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.Rollable.Companion.defaultGetBaseDropRate
import dtx.core.Rollable.Companion.defaultOnSelect
import dtx.core.flattenToList
import dtx.table.Table
import dtx.table.Table.Companion.defaultRollModifier
import dtx.util.NoTransform
import kotlin.random.Random

public interface MultiChanceTable<T, R>: Table<T, R> {

    override val tableEntries: List<ChanceRollable<T, R>>

    public val maxRollChance: Double get() = 100.0 + Double.MIN_VALUE
}

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