package rs_tables
import Item
import Player
import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.Rollable
import dtx.core.flattenToList

infix fun Int.outOf(other: Int) = toDouble() / other.toDouble()

data class RSDropTable(
    val identifier: String,
    val guaranteed: RSGuaranteedTable = RSGuaranteedTable.Empty,
    val preRoll: RSPreRollTable = RSPreRollTable.Empty,
    val mainTable: RSWeightedTable = RSWeightedTable.Empty,
    val tertiaries: RSPreRollTable = RSPreRollTable.Empty,
): RSTable() {

    override val tableEntries: Collection<Rollable<Player, Item>> = emptyList()

    override val ignoreModifier: Boolean = true

    override fun roll(target: Player, otherArgs: ArgMap): RollResult<Item> {
        val results = mutableListOf<RollResult<Item>>()
        results.add(guaranteed.roll(target, otherArgs))
        results.add(preRoll.roll(target, otherArgs))
        results.add(mainTable.roll(target, otherArgs))
        results.add(tertiaries.roll(target, otherArgs))
        return results.flattenToList()
    }
}
