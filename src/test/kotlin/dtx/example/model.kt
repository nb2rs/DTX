package dtx.example

import dtx.example.rs_tables.ChampionType
import dtx.example.rs_tables.ClueTier

sealed interface Item {

    val itemId: String

    val itemAmount: Int

    companion object {
        operator fun invoke(itemId: String, amount: Int = 1): Item = ConcreteItem(itemId, amount)
        operator fun invoke(itemId: String, min: Int, max: Int): Item = RandAmtItem(itemId, min, max)
        operator fun invoke(itemId: String, range: RandomIntRange): Item = RandAmtItem(itemId, range.start, range.endInclusive)
        operator fun invoke(itemId: String, range: IntRange): Item = Item(itemId, range.toRandomIntRange())

    }

    fun copyItem() = if (this is ConcreteItem) {
        copy()
    } else {
        this as RandAmtItem
        copy()
    }
}

data class RandAmtItem(
    override val itemId: String,
    internal val minAmount: Int,
    internal val maxAmount: Int,
): Item {

    internal val range = (minAmount ..< maxAmount).toRandomIntRange()

    override val itemAmount: Int get() = range.random()
}

data class ConcreteItem(
    override val itemId: String,
    override val itemAmount: Int = 1
): Item


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
