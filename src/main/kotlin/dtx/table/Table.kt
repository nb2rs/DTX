package dtx.table

import dtx.core.Rollable

public interface Table<T, R>: Rollable<T, R> {

    public val tableIdentifier: String

    public val tableEntries: Collection<Rollable<T, R>>

    public fun selectEntries(byTarget: T): Collection<Rollable<T, R>> {
        return tableEntries.filter { it.includeInRoll(byTarget) }
    }

    public fun rollModifier(target: T, percentage: Double): Double {
        return defaultRollModifier(percentage)
    }

    public companion object {

        public fun <T> defaultRollModifier(target: T, percentage: Double = 0.0): Double {
            return 1.0 + (percentage / 100.0)
        }
    }
}
