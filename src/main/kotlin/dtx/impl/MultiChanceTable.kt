package dtx.impl

import dtx.core.ArgMap
import dtx.core.BaseDroprate
import dtx.core.OnSelect
import dtx.core.RollModifier
import dtx.core.RollResult
import dtx.core.Rollable.Companion.defaultGetBaseDropRate
import dtx.core.Rollable.Companion.defaultOnSelect
import dtx.core.ShouldRoll
import dtx.core.defaultShouldRoll
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
    protected val shouldRollFunc: ShouldRoll<T> = ::defaultShouldRoll,
    protected val rollModifierFunc: RollModifier<T> = ::defaultRollModifier,
    protected val getBaseDropRateFunc: BaseDroprate<T> = ::defaultGetBaseDropRate,
    protected val onSelectFunc: OnSelect<T, R> = ::defaultOnSelect
): MultiChanceTable<T, R> {

    public override val tableEntries: List<ChanceRollable<T, R>> = entries.map(NoTransform())

    override fun shouldRoll(target: T): Boolean {
        return shouldRollFunc(target)
    }

    public override fun onSelect(target: T, result: RollResult<R>) {
        return onSelectFunc(target, result)
    }

    public override fun rollModifier(target: T, percentage: Double): Double {
        return rollModifierFunc(target, percentage)
    }

    public override fun getBaseDropRate(target: T): Double {
        return getBaseDropRateFunc(target)
    }

    public override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

        if (!shouldRoll(target)) {
            return RollResult.Nothing()
        }

        if (tableEntries.isEmpty()) {
            return RollResult.Nothing()
        }

        val pickedEntries = mutableListOf<ChanceRollable<T, R>>()

        val modifier = rollModifier(target, getBaseDropRate(target))

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
