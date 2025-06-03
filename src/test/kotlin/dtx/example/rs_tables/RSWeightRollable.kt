package dtx.example.rs_tables

import dtx.example.Item
import dtx.example.Player
import dtx.core.RollResult
import dtx.core.Rollable
import kotlin.random.Random

class RSWeightRollable(
    val weight: Int,
    val rollable: Rollable<Player, Item>
): Rollable<Player, Item> by rollable {

    fun rsRoll(target: Player): RollResult<Item> {

        val rolled = Random.nextInt(0, weight)

        if (rolled == 0) {
            return rollable.roll(target)
        }

        return RollResult.Nothing()
    }
}
