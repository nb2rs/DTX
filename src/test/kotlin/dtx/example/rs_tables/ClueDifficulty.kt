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