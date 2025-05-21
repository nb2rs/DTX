package examples

import Item
import Player
import dtx.core.RollResult
import dtx.example.multiChanceTable
import dtx.example.percent
import dtx.example.weightedTable
import randTo
import rs_tables.sendMessage

/**
 * Simple chicken example with a twist
 */
val multiChanceTableExample = multiChanceTable<Player, Item> {

    name("Chicken")

    100.percent chance Item("Bones")
    100.percent chance Item("Raw Chicken")
    75.percent chance weightedTable {

        300 weight Item("Feather", 5)
        100 weight Item("Feather", 15)

        1 weight {

            onSelect { player, result ->

                result as RollResult.Single<Item>
                player.sendMessage("what the cluck, ${result.result.itemAmount} feathers?")
            }

            Item("Feather", 1_000_000 randTo 100_000_000)
        }
    }
}