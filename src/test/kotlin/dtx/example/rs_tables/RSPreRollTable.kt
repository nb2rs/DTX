package dtx.example.rs_tables

import dtx.core.ShouldRoll
import dtx.core.defaultShouldRoll
import dtx.impl.ChanceRollable
import dtx.impl.MultiChanceTable
import dtx.impl.MultiChanceTableBuilder
import dtx.impl.MultiChanceTableImpl
import dtx.impl.Percent

class RSPreRollTable<T, R>(
    tableIdentifier: String,
    tableEntries: List<ChanceRollable<T, R>>,
    shouldRollFunc: ShouldRoll<T> = ::defaultShouldRoll,
): RSTable<T, R>, MultiChanceTable<T, R> by MultiChanceTableImpl<T, R>(
    tableIdentifier, tableEntries, shouldRollFunc
) {
    companion object {
        val EmptyTable = RSPreRollTable<Any?, Any?>("", emptyList()) { false }
        fun <T, R> Empty() = EmptyTable as RSPreRollTable<T, R>
    }
}

class RSPrerollTableBuilder<T, R>: MultiChanceTableBuilder<T, R>() {

    infix fun Int.outOf(other: Int) = Percent(toDouble() / other.toDouble())

    override fun build(): RSPreRollTable<T, R> = RSPreRollTable(
        tableName,
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
