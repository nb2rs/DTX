package dtx.example.rs_tables

import dtx.example.Item
import dtx.example.Player
import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.Rollable
import dtx.core.SingleRollableBuilder
import dtx.core.flattenToList
import dtx.core.singleRollable
import kotlin.collections.forEach


class RSGuaranteedTable(
    public val tableIdentifier: String,
    public override val tableEntries: Collection<Rollable<Player, Item>>,
    public override val ignoreModifier: Boolean = true,
): RSTable() {
    override fun roll(target: Player, otherArgs: ArgMap): RollResult<Item> {
        if (tableEntries.isEmpty()) {
            return RollResult.Nothing()
        }

        val rollResults = mutableListOf<RollResult<Item>>()
        tableEntries.forEach { tableEntry ->
            rollResults.add(tableEntry.roll(target, otherArgs))
        }
        return rollResults.flattenToList()
    }

    companion object {
        val Empty = RSGuaranteedTable("", emptyList())
    }
}

public class RSGuaranteedTableBuilder {
    public var tableIdentifier: String = ""
    private val tableEntries = mutableListOf<Rollable<Player, Item>>()
    public var ignoreModifier = false

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
        tableEntries = tableEntries,
        ignoreModifier = ignoreModifier
    )
}

fun rsGuaranteedTable(builder: RSGuaranteedTableBuilder.() -> Unit): RSGuaranteedTable {
    val builder = RSGuaranteedTableBuilder()
    builder.builder()
    return builder.build()
}
