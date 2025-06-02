package dtx.example.rs_tables

import dtx.example.Item
import dtx.example.Player
import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.Rollable
import dtx.core.SingleRollableBuilder
import dtx.core.flattenToList
import dtx.core.singleRollable
import dtx.impl.ChanceRollable
import dtx.impl.MultiChanceTable
import dtx.impl.MultiChanceTableBuilder
import dtx.impl.MultiChanceTableImpl
import dtx.impl.Percent
import dtx.impl.WeightedRollable
import dtx.impl.WeightedRollableImpl
import kotlin.collections.forEach
import kotlin.random.Random

class RSPreRollTable(
    tableIdentifier: String,
    tableEntries: List<ChanceRollable<Player, Item>>
): RSTable, MultiChanceTable<Player, Item> by MultiChanceTableImpl<Player, Item>(tableIdentifier, tableEntries) {
    companion object {
        val Empty = RSPreRollTable("", emptyList())
    }
}

class RSPrerollTableBuilder: MultiChanceTableBuilder<Player, Item>() {

    infix fun Int.outOf(other: Int) = Percent(toDouble() / other.toDouble())

    override fun build(): RSPreRollTable = RSPreRollTable(
        tableName,
        entries
    )
}

fun rsPrerollTable(builder: RSPrerollTableBuilder.() -> Unit): RSPreRollTable {
    val builder = RSPrerollTableBuilder()
    builder.builder()
    return builder.build()
}

fun rsTertiaryTable(builder: RSPrerollTableBuilder.() -> Unit): RSPreRollTable = rsPrerollTable(builder)
