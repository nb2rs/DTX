package dtx.example

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.Rollable
import dtx.core.rollableHooks
import dtx.core.singleRollable
import dtx.example.scraps
import dtx.impl.chain.ChainRollableHooksBuilder
import dtx.impl.chain.ChainedTableHooksBuilder
import dtx.impl.chain.chainedTable
import kotlin.random.Random

// Example A: Loot chest with luck, pity, and anti-duplication
// Demonstrates usage of ALL hooks across ChainedTableHooks, ChainRollableHooks, and RollableHooks

data class PlayerContext(
    val luck: Int,
    var pityRare: Int,
    val owned: Set<String>,
    val dailyGuaranteed: Boolean
)

data class ItemDrop(val itemId: String, val qty: Int)

private val tableHooks = ChainedTableHooksBuilder<PlayerContext, ItemDrop>()
    .apply {
        baseRollFor { target ->
            println("[table:baseRollFor] target=$target -> 0.0")
            0.0
        }
        modifyRollFor { target, base ->
            println("[table:modifyRoll] target=$target base=$base -> $base")
            base
        }
        shouldInclude { player, args ->
            println("[table:includeInRoll] -> true")
            true
        }
        vetoRoll { player, args ->
            println("[table:vetoRoll] -> false")
            false
        }
        onVeto { player ->
            println("[table:onRollVetoed]")
            RollResult.Nothing()
        }
        transform { player, res ->
            println("[table:transformResult] $res -> $res")
            res
        }
        onRollCompleted { player, args, res ->
            println("[table:onRollCompleted] result=$res")
        }

        /** chain specific hooks */
        onChainStart { player ->
            println("[chain:start] LootChest for $player")
        }
        onEachLink { player, link, rolled, threshold, passed ->
            println("[chain:link] base=${link.base} outOf=${link.rollChance} rolled=$rolled<th=$threshold -> passed=$passed")
        }
        onChainEnd { player, res ->
            println("[chain:end] result=$res")
        }
    }
    .build()

// Build link rollables
val rareRollable: Rollable<PlayerContext, ItemDrop> = singleRollable {
    result(ItemDrop("rare_skin", 1))
}
val uncommonRollable: Rollable<PlayerContext, ItemDrop> = singleRollable {
    result(ItemDrop("uncommon_pack", 1))
}
val commonRollable: Rollable<PlayerContext, ItemDrop> = singleRollable {
    result(ItemDrop("common_pack", 1))
}
val scraps: Rollable<PlayerContext, ItemDrop> = singleRollable {
    result(ItemDrop("random_scraps", 1))
}

// RARE HOOKS
private val rareHooks = ChainRollableHooksBuilder<PlayerContext, ItemDrop>().apply {
    shouldInclude { player, args ->
        val include = "rare_skin" !in player.owned
        println("[rare:includeInRoll] -> $include")
        include
    }
    vetoRoll { player, args ->
        val veto = player.dailyGuaranteed && player.pityRare > 10
        println("[rare:vetoRoll] -> $veto")
        veto
    }
    onVeto { player ->
        println("[rare:onRollVetoed] granting rare immediately")
        RollResult.Single(ItemDrop("rare_skin", 1))
    }
    transform { player, result ->
        val transformed = when (result) {
            is RollResult.Single -> result.copy(result = result.result.copy(qty = result.result.qty + if (player.luck >= 20) 1 else 0))
            else -> result
        }
        println("[rare:transformResult] $result -> $transformed")
        transformed
    }
    onRollCompleted { player, args, result ->
        println("[rare:onRollCompleted] $result")
    }
    /** rare chain hooks */
    adjustChance { player, base, outOf ->
        val pityBoost = (player.pityRare / 5) // +1 base per 5 pity
        val luckBoost = (player.luck / 10)    // +1 base per 10 luck
        val adjusted = (base + pityBoost + luckBoost) to outOf
        println("[rare:adjustChance] ($base/$outOf) -> (${adjusted.first}/${adjusted.second})")
        adjusted
    }
    skipLink { player ->
        val skip = false
        println("[rare:skipLink] -> $skip")
        skip
    }
    nextOverride { player, next ->
        println("[rare:nextOverride] -> next")
        next
    }
    onLinkEvaluated { player, rolled, threshold, passed ->
        println("[rare:onLinkEvaluated] rolled=$rolled<th=$threshold -> passed=$passed")
    }
}.build()

val tableExample = chainedTable {

    // if you think about it this is the rarest item here :)
    defaultRoll(scraps)

    5 outOf 100 rolls rareRollable
    25 outOf 100 rolls uncommonRollable
    99 outOf 100 rolls commonRollable
}

fun runChainedTableExampleA() {
    val ctx = PlayerContext(
        luck = Random.nextInt(1, 100),
        pityRare = 7,
        owned = setOf("starter_skin"),
        dailyGuaranteed = false
    )

    println("=== Running LootChest Example A ===")
    repeat(3) { i ->
        println("-- Roll #$i --")
        val result = lootChest.roll(ctx, ArgMap.Empty)
        println("[demo] roll -> $result\n")
    }
}

fun main() {
    runChainedTableExampleA()
}