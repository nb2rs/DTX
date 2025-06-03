package dtx.example.rs_tables

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.ShouldRoll
import dtx.core.defaultShouldRoll
import dtx.impl.WeightedRollable
import dtx.impl.WeightedTable
import kotlin.random.Random

class RSWeightedTable<T, R>(
    public val tableIdentifier: String,
    entries: List<WeightedRollable<T, R>>,
    private val shouldRollFunc: ShouldRoll<T> = ::defaultShouldRoll
): RSTable<T, R>, WeightedTable<T, R> {

    public override val tableEntries: List<RSWeightEntry<T, R>> = buildList {

        var total = 0

        entries.map {

            val upper = total + it.weight
            val entry = RSWeightEntry(total, upper.toInt(), it)
            total = entry.rangeEnd
            add(entry)
        }
    }

    public override val maxRoll: Double
        get() = tableEntries.maxOf { it.rangeEnd }.toDouble()

    override fun shouldRoll(target: T): Boolean {
        return shouldRollFunc(target)
    }

    override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

        if (!shouldRoll(target)) {
            return RollResult.Nothing()
        }

        if (tableEntries.isEmpty()) {
            return RollResult.Nothing()
        }

        if (tableEntries.size == 1) {
            return tableEntries.first().roll(target, otherArgs)
        }

        val roll = Random.nextInt(0, maxRoll.toInt())

        tableEntries.forEach {

            if (it checkWeight roll) {
                return it.roll(target, otherArgs)
            }
        }
        return RollResult.Nothing()
    }

    companion object {

        val EmptyTable = RSWeightedTable<Any?, Any?>("", emptyList()) { false }

        fun <T, R> Empty() = EmptyTable as RSWeightedTable<T, R>
    }
}
