package dtx.example.rs_tables

import dtx.impl.chance.*
import dtx.impl.misc.Percent
import dtx.table.TableHooks

class RSPreRollTable<T, R>(
    tableIdentifier: String,
    tableEntries: List<ChanceRollable<T, R>>,
    tableHooks: TableHooks<T, R> = TableHooks.Default(),
): RSTable<T, R>, MultiChanceTable<T, R> by MultiChanceTableImpl<T, R>(
    tableIdentifier, tableEntries, tableHooks
) {
    companion object {
        val EmptyTable = RSPreRollTable<Any?, Any?>("", emptyList())
        fun <T, R> Empty() = EmptyTable as RSPreRollTable<T, R>
    }
}

class RSPrerollTableBuilder<T, R>: MultiChanceTableBuilder<T, R>() {

    infix fun Int.outOf(other: Int) = Percent((toDouble() / other.toDouble()) * 100.0)

    override fun build(): RSPreRollTable<T, R> = RSPreRollTable(
        tableIdentifier,
        entries
    )
}

inline fun <T, R> rsPrerollTable(block: RSPrerollTableBuilder<T, R>.() -> Unit): RSPreRollTable<T, R> {

    val builder = RSPrerollTableBuilder<T, R>()
    builder.apply(block)

    return builder.build()
}

inline fun <T, R> rsTertiaryTable(block: RSPrerollTableBuilder<T, R>.() -> Unit): RSPreRollTable<T, R> {
    return rsPrerollTable(block)
}
