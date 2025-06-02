package dtx.example.rs_tables

import dtx.example.Item
import dtx.example.Player
import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.impl.WeightedRollable
import dtx.impl.WeightedTable
import kotlin.random.Random

class RSWeightedTable(
    public val tableIdentifier: String,
    entries: List<WeightedRollable<Player, Item>>,
): RSTable, WeightedTable<Player, Item> {

    public override val tableEntries: List<RSWeightEntry> = buildList {
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

    override fun roll(target: Player, otherArgs: ArgMap): RollResult<Item> {
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
        val Empty = RSWeightedTable("", emptyList())
    }
}
