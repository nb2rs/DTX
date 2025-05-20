import dtx.core.singleRollable
import rs_tables.ChampionType
import kotlin.random.Random

data class Item(
    val itemId: String,
    val itemAmount: Int = 1
)

class Player(
    val username: String,
    val dropRateBonus: Double = 0.0,
    val bank: Collection<Item> = listOf(),
    val inventory: Collection<Item> = listOf(),
    val questPoints: Int = 0,
    val currentWorld: Int = 1,
    val hasScrollCompleted: MutableMap<ChampionType, Boolean> = buildMap {
        ChampionType.entries.forEach { put(it, false) }
    }.toMutableMap()
) {
    fun posesses(item: Item): Boolean = item in inventory || item in bank
    fun isOnMemberWorld(): Boolean = currentWorld > 1
    fun hasChampionScrollComplete(type: ChampionType): Boolean = hasScrollCompleted[type]!!
}

val player = Player("player")


@JvmInline
value class RandomIntRange(val wrappedRange: IntRange): ClosedRange<Int> by wrappedRange, OpenEndRange<Int> by wrappedRange, Iterable<Int> by wrappedRange {

    override val start: Int get() = wrappedRange.first

    override val endInclusive: Int get() = wrappedRange.endInclusive

    override fun contains(value: Int) = wrappedRange.contains(value)

    override fun isEmpty() = wrappedRange.isEmpty()

    fun random(withRandom: Random = Random) = wrappedRange.random(withRandom)
}

inline fun IntRange.toRandomIntRange() = RandomIntRange(this)

infix fun Int.randTo(endInclusive: Int) = RandomIntRange(this .. endInclusive)

fun Item(itemId: String, amount: RandomIntRange) = singleRollable<Player, Item> {
    result {
        Item(itemId, amount.random())
    }
}
