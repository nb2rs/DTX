package dtx.example

import dtx.core.ArgMap
import dtx.impl.MetaEntryFilter
import dtx.impl.MetaRollable
import dtx.impl.MetaWeightedRollable
import dtx.impl.metaWeightedTable
import kotlin.random.Random

enum class OreModAmount(val modifyBy: Double) {
    Miniscule(0.125), Slight(0.25), Small(0.5), Moderate(1.0), Medium(2.0), Large(4.0);
}

enum class MetaModDirection(val operation: (Double, Double) -> Double) {
    Increase(Double::plus), Decrease(Double::minus);
}

data class OreFilter(
    val idEquals: String,
    val direction: MetaModDirection,
    val type: OreModAmount,
): MetaEntryFilter<Player, Item> {

    override fun filterEntry(modifier: String): Boolean {
        return modifier == idEquals
    }

    override fun modifyEntry(entry: MetaRollable<Player, Item>) {
        entry as MetaWeightedRollable<Player, Item>
        println("Modifying ${entry.identifier} by ${type.modifyBy} (${entry.currentWeight} -> ${direction.operation(entry.currentWeight, direction.operation(0.0, type.modifyBy))})")
        entry.increaseCurrentWeightBy(direction.operation(0.0, type.modifyBy))
    }
}


/**
 * Example of a """realistic""" mining node.
 * The more you mine it the less of the other stuff you get.
 * But it also re-ups the things you don't get so not actually
 */
val ExampleOreNode = metaWeightedTable<Player, Item> {

    name("Example Ore Node")
    ignoreModifier(true)

    100.0 weight {

        id("stone")

        rollable {
            Item("Stone", Random.nextInt(1, 4))
        }

        minWeight(75.0)
        maxWeight(133.0)

        addFilter(OreFilter("stone", MetaModDirection.Decrease, OreModAmount.Slight))
        addFilter(OreFilter("impure", MetaModDirection.Increase, OreModAmount.Moderate))
        addFilter(OreFilter("pure", MetaModDirection.Increase, OreModAmount.Small))
        addFilter(OreFilter("gem", MetaModDirection.Increase, OreModAmount.Miniscule))
    }

    33.33 weight {

        id("impure")

        rollable {
            Item("Impure Ore", Random.nextInt(1, 3))
        }

        minWeight(16.66)
        maxWeight(50.00)

        addFilter(OreFilter("stone", MetaModDirection.Increase, OreModAmount.Small))
        addFilter(OreFilter("impure", MetaModDirection.Decrease, OreModAmount.Moderate))
        addFilter(OreFilter("pure", MetaModDirection.Increase, OreModAmount.Small))
        addFilter(OreFilter("gem", MetaModDirection.Increase, OreModAmount.Miniscule))
    }

    10.00 weight {

        id("pure")

        rollable {
            Item("Pure Ore", Random.nextInt(1, 2))
        }

        minWeight(5.00)
        maxWeight(16.66)

        addFilter(OreFilter("stone", MetaModDirection.Increase, OreModAmount.Large))
        addFilter(OreFilter("impure", MetaModDirection.Increase, OreModAmount.Slight))
        addFilter(OreFilter("pure", MetaModDirection.Decrease, OreModAmount.Medium))
        addFilter(OreFilter("gem", MetaModDirection.Increase, OreModAmount.Small))
    }

    3.00 weight {

        id("gem")

        rollable(Item("Gem"))

        minWeight(1.00)
        maxWeight(5.00)

        addFilter(OreFilter("stone", MetaModDirection.Increase, OreModAmount.Large))
        addFilter(OreFilter("impure", MetaModDirection.Increase, OreModAmount.Medium))
        addFilter(OreFilter("pure", MetaModDirection.Increase, OreModAmount.Moderate))
        addFilter(OreFilter("gem", MetaModDirection.Decrease, OreModAmount.Large))
    }
}

fun main() {
    repeat(1000) {
        println("-------------------------")
        println("Weights at $it rolls")
        ExampleOreNode.tableEntries.forEach { item ->
            println("${item.identifier}: ${item.currentWeight}")
        }
        println("-------------------------")
        readlnOrNull()

        val result = ExampleOreNode.roll(examplePlayer, ArgMap())
        println("Result: $result")
    }
}
