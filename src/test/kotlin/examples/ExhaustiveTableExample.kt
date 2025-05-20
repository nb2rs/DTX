package examples

import Item
import Player
import dtx.example.uniformExhaustiveTable
import dtx.example.weightedExhaustiveTable

/**
 * Could be useful for something like a lottery or a raffle or low-effort encounter picking
 */
val uniformExhaustiveTableExample = uniformExhaustiveTable<Player, Item> {

    name("Goblin dungeon encounters")

    1 rolls Item("Goblin Head Honcho")

    3 rolls Item("Goblin Jockey")

    6 rolls Item("Goblin Horde")

    10 rolls Item("Lone goblin")
}

/**
 * Same thing, but with the [WeightedExhaustiveTable], the items are weighted by how many rolls they have left.
 */
val weightedExhaustiveTableExample = weightedExhaustiveTable<Player, Item> {

    name("Card raffle")

    1 rolls Item("Jokette")

    2 rolls Item("Ace of Spades")
    2 rolls Item("Ace of Hearts")
    2 rolls Item("Ace of Diamonds")
    2 rolls Item("Ace of Clubs")

    4 rolls Item("King of Spades")
    4 rolls Item("King of Hearts")
    4 rolls Item("King of Diamonds")
    4 rolls Item("King of Clubs")
}