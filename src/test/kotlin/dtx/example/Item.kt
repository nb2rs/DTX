package dtx.example

import dtx.core.singleRollable

data class Item(
    val itemId: String,
    val itemAmount: Int = 1
)

fun Item(itemId: String, amount: RandomIntRange) = singleRollable<Player, Item> {

    result {
        Item(itemId, amount.random())
    }
}