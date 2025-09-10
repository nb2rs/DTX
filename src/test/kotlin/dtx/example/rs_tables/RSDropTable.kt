package dtx.example.rs_tables
import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.Rollable
import dtx.core.flattenToList
import dtx.table.TableHooks

class RSDropTable<T, R>(
    public override val tableIdentifier: String,
    private val guaranteed: RSGuaranteedTable<T, R> = RSGuaranteedTable.Empty(),
    private val preRoll: RSPreRollTable<T, R> = RSPreRollTable.Empty(),
    private val mainTable: RSWeightedTable<T, R> = RSWeightedTable.Empty(),
    private val tertiaries: RSPreRollTable<T, R> = RSPreRollTable.Empty(),
    private val hooks: TableHooks<T, R> = TableHooks.Default(),
): RSTable<T, R>, TableHooks<T, R> by hooks {

    override val tableEntries: Collection<Rollable<T, R>> = listOf(guaranteed, preRoll, mainTable, tertiaries)

    override fun selectResult(target: T, otherArgs: ArgMap): RollResult<R> {
        val results = mutableListOf<RollResult<R>>()
        results.add(guaranteed.roll(target, otherArgs))
        results.add(preRoll.roll(target, otherArgs))
        results.add(mainTable.roll(target, otherArgs))
        results.add(tertiaries.roll(target, otherArgs))

        return results.flattenToList()
    }
}
