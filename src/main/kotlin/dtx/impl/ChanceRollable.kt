package dtx.impl

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.Rollable

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

        override val chance: Double = 0.0

        override val rollable: Rollable<Any?, Any?> = Rollable.Empty()
    }

    public companion object {

        public fun <T, R> Empty(): ChanceRollable<T, R> {
            return Empty as ChanceRollable<T, R>
        }
    }
}

public data class ChanceRollableImpl<T, R>(
    override val chance: Double,
    override val rollable: Rollable<T, R>
): ChanceRollable<T, R>
