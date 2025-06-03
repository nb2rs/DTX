package dtx.example.rs_tables

import dtx.impl.WeightedTableBuilder

class RSWeightedTableBuilder<T, R>: WeightedTableBuilder<T, R>() {

    public override fun build(): RSWeightedTable<T, R> {
        return RSWeightedTable(
            tableName,
            entries,
            getDropRateFunc,
            getRollModFunc,
            shouldRollFunc
        )
    }
}

fun <T, R> rsWeightedTable(block: RSWeightedTableBuilder<T, R>.() -> Unit): RSWeightedTable<T, R> {

    val builder = RSWeightedTableBuilder<T, R>()
    builder.apply(block)

    return builder.build()
}
