package dtx.example.rs_tables

import dtx.example.Item
import dtx.example.Player
import dtx.impl.WeightedTableBuilder

class RSWeightedTableBuilder: WeightedTableBuilder<Player, Item>() {
    public override fun build(): RSWeightedTable {
        return RSWeightedTable(
            tableName,
            entries
        )
    }
}

fun rsWeightedTable(builder: RSWeightedTableBuilder.() -> Unit): RSWeightedTable = RSWeightedTableBuilder().apply(builder).build()