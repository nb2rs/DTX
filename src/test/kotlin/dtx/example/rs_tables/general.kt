package dtx.example.rs_tables

import dtx.example.Item
import dtx.example.Player
import dtx.core.singleRollable

enum class ClueDifficulty(val isFreeToPlay: Boolean = false) {
    Beginner(true), Easy, Medium, Hard, Elite, Master;

    val item get() = Item("clue_scroll_(${this.name.lowercase()})")
}

fun clueScrollDrop(difficulty: ClueDifficulty) = singleRollable<Player, Item> {

    val item = difficulty.item

    shouldRoll { player ->

        if (difficulty.isFreeToPlay) {
            return@shouldRoll true
        }

        if (!player.isOnMemberWorld()) {
            return@shouldRoll false
        }

        !player.posesses(item)
    }

    result(difficulty.item.copy())
}

enum class ChampionType {
    Imp, Goblin, Skeleton, Zombie,
    Giant, Hobgoblin, Ghoul, EarthWarrior,
    Jogre, LesserDemon, Human
}

fun championScroll(type: ChampionType) = singleRollable<Player, Item> {

    val scrollItem = Item("${type.name.lowercase()}_champion_scroll")

    shouldRoll { target ->
        if (target.hasChampionScrollComplete(type) || target.posesses(scrollItem)) {
            target.sendMessage("You have a funny feeling that you would have recieved a Champion's scroll...")
            return@shouldRoll false
        }
        target.questPoints > 32
                && target.isOnMemberWorld()
                && !target.hasChampionScrollComplete(type)

    }

    onSelect { target, result ->
        target.sendMessage("A Champion's scroll falls to the ground as you slay your opponent.")
    }

    result(scrollItem)
}

val longCurvedTable = rsWeightedTable {
    name("Long and Curved Bone table")
    400 weight Item("long_bone")
    1 weight Item("curved_bone")
}

fun Player.sendMessage(message: String) {
    println("[to: $username]: $message")
}
