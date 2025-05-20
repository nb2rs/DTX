package examples

import Item
import Player
import dtx.example.multiChanceTable
import dtx.example.percent
import dtx.example.weightedTable
import randTo

/**
 * Very simple
 */
val multiChanceTableExample = multiChanceTable<Player, Item> {

    name("Runescape Chicken, with a twist")

    100.percent chance Item("Bones")

    100.percent chance Item("Raw Chicken")

    75.percent chance weightedTable {
        3 weight Item("Feather", 5)
        1 weight Item("Feather", 15)
        1 weight Item("Feather", 1_000_000 randTo 100_000_000)
    }
}