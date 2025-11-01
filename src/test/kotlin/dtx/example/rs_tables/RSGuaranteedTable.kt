package dtx.example.rs_tables

import dtx.core.RollResult
import dtx.core.Rollable
import dtx.core.Single
import dtx.core.SingleRollableBuilder
import dtx.core.singleRollable
import dtx.impl.chance.*
import dtx.table.AbstractTableBuilder
import dtx.table.DefaultTableHooksBuilder
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

public class RSGuaranteedTableBuilder<T, R>: AbstractTableBuilder<
        T,
        R,
        Rollable<T, R>,
        RSGuaranteedTable<T, R>,
        TableHooks<T, R>,
        DefaultTableHooksBuilder<T, R>,
        RSGuaranteedTableBuilder<T, R>
>(DefaultTableHooksBuilder.new()) {

    protected override val entries: MutableCollection<Rollable<T, R>> = mutableListOf()

    fun add(rollable: Rollable<T, R>): RSGuaranteedTableBuilder<T, R> {
        addEntry(rollable)
        return this
    }

    fun add(block: SingleRollableBuilder<T, R>.() -> Unit): RSGuaranteedTableBuilder<T, R> {
        val rollable = singleRollable(block)
        addEntry(rollable)
        return this
    }

    fun add(item: R): RSGuaranteedTableBuilder<T, R> {
        add(Single(item))
        return this
    }

    init {
        construct {
            RSGuaranteedTable(tableIdentifier, entries, hooks.build())
        }
    }
}

fun <T, R> rsGuaranteedTable(block: RSGuaranteedTableBuilder<T, R>.() -> Unit): RSGuaranteedTable<T, R> {

    val builder = RSGuaranteedTableBuilder<T, R>()
    builder.apply(block)

    return builder.build()
}

fun <T, R> guaranteedSingle(result: R): RSGuaranteedTable<T, R> = rsGuaranteedTable {
    add(result)
}
