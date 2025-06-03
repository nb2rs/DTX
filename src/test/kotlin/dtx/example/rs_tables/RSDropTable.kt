package dtx.example.rs_tables
import dtx.example.Player
import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.Rollable
import dtx.core.ShouldRoll
import dtx.core.defaultShouldRoll
import dtx.core.flattenToList

data class RSDropTable<T, R>(
    val identifier: String,
    val guaranteed: RSGuaranteedTable<T, R> = RSGuaranteedTable.Empty(),
    val preRoll: RSPreRollTable<T, R> = RSPreRollTable.Empty(),
    val mainTable: RSWeightedTable<T, R> = RSWeightedTable.Empty(),
    val tertiaries: RSPreRollTable<T, R> = RSPreRollTable.Empty(),
    private val shouldRollFunc: ShouldRoll<T> = ::defaultShouldRoll,
): RSTable<T, R> {

    override val tableEntries: Collection<Rollable<T, R>> = emptyList()

    override fun shouldRoll(target: T): Boolean {
        return shouldRollFunc(target)
    }

    override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

        val results = mutableListOf<RollResult<R>>()
        results.add(guaranteed.roll(target, otherArgs))
        results.add(preRoll.roll(target, otherArgs))
        results.add(mainTable.roll(target, otherArgs))
        results.add(tertiaries.roll(target, otherArgs))

        return results.flattenToList()
    }
}
