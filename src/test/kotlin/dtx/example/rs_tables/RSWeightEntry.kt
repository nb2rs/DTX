package dtx.example.rs_tables

import dtx.example.Item
import dtx.example.Player
import dtx.core.Rollable
import dtx.impl.WeightedRollable
import dtx.impl.WeightedRollableImpl

class RSWeightEntry(
    val rangeStart: Int,
    val rangeEnd: Int,
    rollable: Rollable<Player, Item>
): WeightedRollable<Player, Item> by WeightedRollableImpl(
    weight = (rangeEnd - rangeStart).toDouble(),
    rollable
) {
    infix fun checkWeight(value: Int): Boolean = value in rangeStart ..< rangeEnd
}