package dtx.example

import dtx.example.rs_tables.ChampionType

class Player(
    val username: String,
    var dropRateBonus: Double = 0.0,
    val bank: Collection<Item> = listOf(),
    val inventory: Collection<Item> = listOf(),
    var questPoints: Int = 0,
    var currentWorld: Int = 1,
    val hasScrollCompleted: MutableMap<ChampionType, Boolean> = buildMap {
        ChampionType.entries.forEach { put(it, false) }
    }.toMutableMap()
) {

    fun posesses(item: Item): Boolean = item in inventory || item in bank

    fun isOnMemberWorld(): Boolean = currentWorld > 1

    fun hasChampionScrollComplete(type: ChampionType): Boolean = hasScrollCompleted[type]!!
}

val examplePlayer = Player("player")