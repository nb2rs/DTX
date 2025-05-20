package rs_tables

import Item
import Player
import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.Rollable
import dtx.core.SingleRollableBuilder
import dtx.core.flattenToList
import dtx.core.singleRollable
import dtx.example.WeightedRollable
import dtx.example.WeightedRollableImpl
import kotlin.collections.forEach
import kotlin.random.Random


class RSPreRollTable(
    public val tableIdentifier: String,
    public override val tableEntries: List<WeightedRollable<Player, Item>>,
): RSTable() {

    override val ignoreModifier: Boolean = true


    override fun roll(target: Player, otherArgs: ArgMap): RollResult<Item> {

        if (tableEntries.isEmpty()) {
            return RollResult.Nothing()
        }

        val results = mutableListOf<RollResult<Item>>()
        tableEntries.forEach { tableEntry ->
            val roll = Random.nextDouble(0.0, 1.0 + Double.MIN_VALUE)
            if (roll <= tableEntry.weight) {
                results.add(tableEntry.roll(target, otherArgs))
            }
        }
        return results.flattenToList()
    }

    companion object {
        val Empty = RSPreRollTable("", emptyList())
    }
}

class RSPrerollTableBuilder {
    public var identifier: String = ""
    private val prerollEntries = mutableListOf<WeightedRollable<Player, Item>>()
    public var ignoreModifier: Boolean = false

    fun identifier(identifier: String): RSPrerollTableBuilder {
        this.identifier = identifier
        return this
    }

    fun ignoreModifier(ignore: Boolean): RSPrerollTableBuilder {
        this.ignoreModifier = ignore
        return this
    }

    fun addEntry(entry: WeightedRollable<Player, Item>): RSPrerollTableBuilder {
        prerollEntries.add(entry)
        return this
    }

    infix fun Double.chance(entry: Rollable<Player, Item>) = addEntry(entry = WeightedRollableImpl(this, entry))

    infix fun Double.chance(builder: SingleRollableBuilder<Player, Item>.() -> Unit) = chance(singleRollable(builder))

    infix fun Double.chance(item: Item) = chance(Rollable.Single(item))

    infix fun Int.weight(entry: Rollable<Player, Item>) = (1 outOf this).chance(entry)

    infix fun Int.weight(builder: SingleRollableBuilder<Player, Item>.() -> Unit) = weight(singleRollable(builder))

    infix fun Int.weight(item: Item) = weight(Rollable.Single(item))

    fun build(): RSPreRollTable = RSPreRollTable(
        identifier,
        prerollEntries
    )
}

fun rsPrerollTable(builder: RSPrerollTableBuilder.() -> Unit): RSPreRollTable {
    val builder = RSPrerollTableBuilder()
    builder.builder()
    return builder.build()
}

fun rsTertiaryTable(builder: RSPrerollTableBuilder.() -> Unit): RSPreRollTable = rsPrerollTable(builder)