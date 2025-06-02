package dtx.example.rs_tables

import dtx.example.Item
import dtx.example.Player
import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.Rollable
import dtx.core.SingleRollableBuilder
import dtx.core.singleRollable
import dtx.impl.WeightedRollable
import dtx.impl.WeightedRollableImpl
import dtx.impl.WeightedTable
import dtx.impl.WeightedTableBuilder
import kotlin.random.Random

class RSWeightEntry(
    val rangeStart: Int,
    val rangeEnd: Int,
    rollable: Rollable<Player, Item>
): WeightedRollable<Player, Item> by WeightedRollableImpl(
    weight = (rangeEnd - rangeStart).toDouble(),
    rollable
) {
    infix fun checkWeight(value: Int): Boolean = value in rangeStart ..< rangeEnd

}

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

class RSWeightedTableBuilder: WeightedTableBuilder<Player, Item>() {
    public override fun build(): RSWeightedTable {
        return RSWeightedTable(
            tableName,
            entries
        )
    }
}

fun rsWeightedTable(builder: RSWeightedTableBuilder.() -> Unit): RSWeightedTable = RSWeightedTableBuilder().apply(builder).build()
