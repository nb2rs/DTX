package dtx.example

import dtx.impl.uniformExhaustiveTable
import dtx.impl.weightedExhaustiveTable


/**
 * Good for simple raffles or encounter table draws or something like that
 */
val uniformExhaustiveTableExample = uniformExhaustiveTable<Player, Item> {

    name("Goblin dungeon encounters")

    1 rolls Item("Goblin Head Honcho")

    3 rolls Item("Goblin Jockey")

    6 rolls Item("Goblin Horde")

    10 rolls Item("Lone goblin")
}


/**
 * Very good for spicy raffles tbh, weights are based on how many rolls each item has
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