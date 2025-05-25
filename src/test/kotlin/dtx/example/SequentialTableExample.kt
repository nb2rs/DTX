package dtx.example

import dtx.impl.sequentialTable

/**
 * Good for stuff like dailies, weeklies, something that has some revolving reward.
 */
val sequentialTableExample = sequentialTable<Player, Item> {

    name("Daily Login Rewards")

    add(Item("Day 1 - Spaghetti Box"))
    add(Item("Day 2 - Some coins", 100 randTo 1_000))
    add(Item("Day 3 - Shield", 1))
    add(Item("Day 4 - Epic Healing Potion of Awesomeness", 1))

}