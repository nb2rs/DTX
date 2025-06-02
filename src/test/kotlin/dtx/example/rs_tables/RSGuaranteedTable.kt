package dtx.example.rs_tables

import dtx.example.Item
import dtx.example.Player
import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.Rollable
import dtx.core.SingleRollableBuilder
import dtx.core.flattenToList
import dtx.core.singleRollable
import dtx.impl.ChanceRollableImpl
import dtx.impl.MultiChanceTable
import dtx.impl.MultiChanceTableImpl
import kotlin.collections.forEach


/**
 * Implements [MultiChanceTable] via the default [MultiChanceTableImpl]
 * Entries are all 100% chance because this table is guaranteed to roll on everything.
 * Whether every [Rollable] will give a non-[RollResult.Nothing] result is not guaranteed, just that the [Rollable] will be rolled
 */
class RSGuaranteedTable(
    tableIdentifier: String,
    tableEntries: Collection<Rollable<Player, Item>>,
): RSTable, MultiChanceTable<Player, Item> by MultiChanceTableImpl(
    tableIdentifier,
    tableEntries.map { ChanceRollableImpl(100.0, it) }
) {
    companion object {
        val Empty = RSGuaranteedTable("", emptyList())
    }
}

public class RSGuaranteedTableBuilder {

    public var tableIdentifier: String = ""

    private val tableEntries = mutableListOf<Rollable<Player, Item>>()

    fun identifier(identifier: String): RSGuaranteedTableBuilder {
        tableIdentifier = identifier
        return this
    }

    fun add(rollable: Rollable<Player, Item>): RSGuaranteedTableBuilder {
        tableEntries.add(rollable)
        return this
    }

    fun add(block: SingleRollableBuilder<Player, Item>.() -> Unit): RSGuaranteedTableBuilder {
        val rollable = singleRollable(block)
        tableEntries.add(rollable)
        return this
    }

    fun add(item: Item): RSGuaranteedTableBuilder {
        add(Rollable.Single(item))
        return this
    }

    fun build(): RSGuaranteedTable = RSGuaranteedTable(
        tableIdentifier = tableIdentifier,
        tableEntries = tableEntries
    )
}

fun rsGuaranteedTable(builder: RSGuaranteedTableBuilder.() -> Unit): RSGuaranteedTable {
    val builder = RSGuaranteedTableBuilder()
    builder.builder()
    return builder.build()
}
