package dtx.example

import dtx.core.singleRollable
import dtx.example.rs_tables.ChampionType

data class Item(
    val itemId: String,
    val itemAmount: Int = 1
)

fun Item(itemId: String, amount: RandomIntRange) = singleRollable<Player, Item> {

    result {
        Item(itemId, amount.random())
    }
}

inline fun Item(itemId: String, amountRange: IntRange) = Item(itemId, amountRange.toRandomIntRange())

enum class Gender {
    PlatelegEnjoyer, PlateskirtEnthusiast;
}

data class Player(
    val username: String,
    var dropRateBonus: Double = 0.0,
    val bank: Collection<Item> = listOf(),
    val inventory: Collection<Item> = listOf(),
    val equipment: Collection<Item> = listOf(),
    var questPoints: Int = 0,
    var currentWorld: Int = 1,
    var gender: Gender = Gender.PlateskirtEnthusiast,
    val hasScrollCompleted: MutableMap<ChampionType, Boolean> = buildMap {
        ChampionType.entries.forEach { put(it, false) }
    }.toMutableMap()
) {

    fun isWearing(item: Item): Boolean = equipment.any { it.itemId == item.itemId }

    fun isWearing(itemId: String): Boolean = equipment.any { it.itemId == itemId }

    fun posesses(item: Item): Boolean = item in inventory || item in bank || item in equipment

    fun isOnMemberWorld(): Boolean = currentWorld > 1

    fun hasChampionScrollComplete(type: ChampionType): Boolean = hasScrollCompleted[type]!!
}

fun Player.sendMessage(message: String) {
    println("[to: $username]: $message")
}


val examplePlayer = Player("player")
