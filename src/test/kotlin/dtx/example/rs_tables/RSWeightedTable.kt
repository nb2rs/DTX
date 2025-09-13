package dtx.example.rs_tables

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.impl.weighted.WeightedRollable
import dtx.impl.weighted.WeightedTable
import dtx.table.TableHooks
import kotlin.random.Random

class RSWeightedTable<T, R>(
    public override val tableIdentifier: String,
    public override val tableEntries: Collection<WeightedRollable<T, R>>,
    private val hooks: TableHooks<T, R> = TableHooks.Default(),
): RSTable<T, R>, WeightedTable<T, R>, TableHooks<T, R> by hooks {

    override fun selectEntries(byTarget: T): List<RSWeightEntry<T, R>>  = buildList {

        var total = 0

        tableEntries.forEach {
            if (it.includeInRoll(byTarget)) {
                val upper = total + it.weight
                val entry = RSWeightEntry(total, upper.toInt(), it.rollable)
                total = entry.rangeEnd
                add(entry)
            }
        }
    }

    public override val maxRoll: Double
        get() = tableEntries.maxOf { it.weight }

    override fun selectResult(target: T, otherArgs: ArgMap): RollResult<R> {

        val entries = selectEntries(target)

        if (tableEntries.isEmpty()) {
            return RollResult.Nothing()
        }

        if (tableEntries.size == 1) {
            return tableEntries.first().roll(target, otherArgs)
        }

        val localMax = entries.maxOf { it.rangeEnd }

        val baseRoll = Random.nextInt(0, localMax)
        val flatMod = rollModifier(target, 0.0)

        val roll = (baseRoll * flatMod).toInt()

        entries.forEach { entry ->
            if (entry checkWeight roll) {
                return entry.roll(target, otherArgs)
            }
        }

        return RollResult.Nothing()
    }

    companion object {

        val EmptyTable = RSWeightedTable<Any?, Any?>("", emptyList())

        fun <T, R> Empty() = EmptyTable as RSWeightedTable<T, R>
    }
}
