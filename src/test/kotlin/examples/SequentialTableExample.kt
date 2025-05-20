package examples

import Item
import Player
import dtx.core.SingleRollableBuilder
import dtx.example.sequentialTable
import randTo

/**
 * Daily login rewards or some type of revolving reward system is good for this type of thing.
 * It will be in the declared order, so you have to do them in the order you want!
 */
val sequentialTableExample = sequentialTable<Player, Item> {

    name("Daily Login Rewards")

    add(Item("Day 1 - Spaghetti Box"))
    add(Item("Day 2 - Coins", 100 randTo 1_000))
    add(Item("Day 3 - Shield", 1))
    add(Item("Day 4 - Epic Healing Potion of Awesomeness", 1))

}