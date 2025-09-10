package dtx.example.rs_tables

import dtx.core.RollResult
import dtx.core.Rollable
import dtx.core.Single
import dtx.core.SingleRollableBuilder
import dtx.core.singleRollable
import dtx.impl.ChanceRollableImpl
import dtx.impl.MultiChanceTable
import dtx.impl.MultiChanceTableImpl
import dtx.table.TableHooks


/**
 * Implements [MultiChanceTable] via the default [MultiChanceTableImpl]
 * Entries are all 100% chance because this table is guaranteed to roll on everything.
 * Whether every [Rollable] will give a non-[RollResult.Nothing] result is not guaranteed, just that the [Rollable] will be rolled
 */
class RSGuaranteedTable<T, R>(
    tableIdentifier: String,
    tableEntries: Collection<Rollable<T, R>>,
    tableHooks: TableHooks<T, R> = TableHooks.Default(),
): RSTable<T, R>, MultiChanceTable<T, R> by MultiChanceTableImpl(
    tableIdentifier,
    tableEntries.map { ChanceRollableImpl(100.0, it) },
    tableHooks
) {
    companion object {
        val EmptyTable = RSGuaranteedTable<Any?, Any?>("", emptyList())
        fun <T, R> Empty() = EmptyTable as RSGuaranteedTable<T, R>
    }
}

public class RSGuaranteedTableBuilder<T, R> {

    public var tableIdentifier: String = ""

    private val tableEntries = mutableListOf<Rollable<T, R>>()

    fun identifier(identifier: String): RSGuaranteedTableBuilder<T, R> {
        tableIdentifier = identifier
        return this
    }

    fun add(rollable: Rollable<T, R>): RSGuaranteedTableBuilder<T, R> {
        tableEntries.add(rollable)
        return this
    }

    fun add(block: SingleRollableBuilder<T, R>.() -> Unit): RSGuaranteedTableBuilder<T, R> {
        val rollable = singleRollable(block)
        tableEntries.add(rollable)
        return this
    }

    fun add(item: R): RSGuaranteedTableBuilder<T, R> {
        add(Single(item))
        return this
    }

    fun build(): RSGuaranteedTable<T, R> = RSGuaranteedTable(
        tableIdentifier = tableIdentifier,
        tableEntries = tableEntries
    )
}

fun <T, R> rsGuaranteedTable(block: RSGuaranteedTableBuilder<T, R>.() -> Unit): RSGuaranteedTable<T, R> {

    val builder = RSGuaranteedTableBuilder<T, R>()
    builder.apply(block)

    return builder.build()
}

fun <T, R> guaranteedSingle(result: R): RSGuaranteedTable<T, R> = rsGuaranteedTable {
    add(result)
}
