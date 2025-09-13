package dtx.example.rs_tables

import dtx.impl.weighted.WeightedTableBuilder


class RSWeightedTableBuilder<T, R>: WeightedTableBuilder<T, R, RSWeightedTable<T, R>>(::RSWeightedTable)

fun <T, R> rsWeightedTable(block: RSWeightedTableBuilder<T, R>.() -> Unit): RSWeightedTable<T, R> {

    val builder = RSWeightedTableBuilder<T, R>()
    builder.apply(block)

    return builder.build()
}
