package dtx.example.rs_tables

import dtx.core.Rollable
import dtx.impl.WeightedRollable
import dtx.impl.WeightedRollableImpl

class RSWeightEntry<T, R>(
    val rangeStart: Int,
    val rangeEnd: Int,
    rollable: Rollable<T, R>
): WeightedRollable<T, R> by WeightedRollableImpl(
    weight = (rangeEnd - rangeStart).toDouble(),
    rollable
) {
    infix fun checkWeight(value: Int): Boolean = value in rangeStart ..< rangeEnd
}