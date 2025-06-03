package dtx.impl

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.Rollable
import dtx.core.ShouldRoll
import dtx.core.defaultShouldRoll

public interface ChanceRollable<T, R>: Rollable<T, R> {

    public val chance: Double
    public val rollable: Rollable<T, R>

    public operator fun component1(): Double {
        return chance
    }

    public operator fun component2(): Rollable<T, R> {
        return rollable
    }

    public override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {
        return rollable.roll(target, otherArgs)
    }

    private data object Empty: ChanceRollable<Any?, Any?> {

        override fun shouldRoll(target: Any?): Boolean {
            return false
        }

        override val chance: Double = 0.0

        override val rollable: Rollable<Any?, Any?> = Rollable.Empty()
    }

    public companion object {

        public fun <T, R> Empty(): ChanceRollable<T, R> {
            return Empty as ChanceRollable<T, R>
        }
    }
}

public class ChanceRollableImpl<T, R>(
    override val chance: Double,
    override val rollable: Rollable<T, R>,
    private val shouldRollFunc: ShouldRoll<T> = ::defaultShouldRoll
): ChanceRollable<T, R> {
    override fun shouldRoll(target: T): Boolean {
        return shouldRollFunc(target)
    }

    override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

        if (!shouldRoll(target)) {
            return RollResult.Nothing()
        }

        return rollable.roll(target, otherArgs)
    }
}
