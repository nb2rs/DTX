package dtx.example.rs_tables

import dtx.example.Item
import dtx.example.Player
import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.Rollable
import dtx.core.SingleRollableBuilder
import dtx.core.singleRollable
import kotlin.random.Random

data class RSWeightEntry(
    val rangeStart: Int,
    val rangeEnd: Int,
    val rollable: Rollable<Player, Item>
): Rollable<Player, Item> by rollable {
    infix fun checkWeight(value: Int): Boolean = value in rangeStart ..< rangeEnd
}

class RSWeightedTable(
    public val tableIdentifier: String,
    entries: List<RSWeightRollable>,
): RSTable() {

    override val ignoreModifier: Boolean = true

    public override val tableEntries: List<RSWeightEntry> = buildList {
        var total = 0
        entries.map {
            val upper = total + it.weight
            val entry = RSWeightEntry(total, upper, it)
            total = upper
            add(entry)
        }
    }

    private val maxRoll = (tableEntries.maxOfOrNull { it.rangeEnd } ?: 0) + 1

    override fun roll(target: Player, otherArgs: ArgMap): RollResult<Item> {
        if (tableEntries.isEmpty()) {
            return RollResult.Nothing()
        }
        if (tableEntries.size == 1) {
            return tableEntries.first().roll(target, otherArgs)
        }
        val roll = Random.nextInt(0, maxRoll)
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

class RSWeightedTableBuilder {
    public var tableIdentifier: String = ""
    private val tableEntries = mutableListOf<RSWeightRollable>()
    public var ignoreModifier = true

    fun identifier(newIdentifier: String): RSWeightedTableBuilder {
        tableIdentifier = newIdentifier
        return this
    }

    fun ignoreModifier(newIgnore: Boolean): RSWeightedTableBuilder {
        ignoreModifier = newIgnore
        return this
    }

    fun addEntry(entry: RSWeightRollable): RSWeightedTableBuilder {
        tableEntries.add(entry)
        return this
    }

    infix fun Int.weight(entry: Rollable<Player, Item>) = addEntry(RSWeightRollable(this, entry))

    infix fun Int.build(builder: SingleRollableBuilder<Player, Item>.() -> Unit) = weight(singleRollable(builder))

    infix fun Int.weight(item: Item) = weight(Rollable.Single(item))

    fun build(): RSWeightedTable = RSWeightedTable(tableIdentifier, tableEntries)
}

fun rsWeightedTable(builder: RSWeightedTableBuilder.() -> Unit): RSWeightedTable = RSWeightedTableBuilder().apply(builder).build()
