package dtx.example.rs_tables

import dtx.example.Item
import dtx.example.Player
import dtx.core.singleRollable

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