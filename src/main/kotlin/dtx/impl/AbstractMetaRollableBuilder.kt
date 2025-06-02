package dtx.impl

import dtx.core.Rollable

public abstract class AbstractMetaRollableBuilder<T, R, E: MetaRollable<T, R>, B: AbstractMetaRollableBuilder<T, R, E, B>> {

    public var identifier: String = ""
    public var rollable: Rollable<T, R> = Rollable.Empty()
    public var initialValue: Double = 1.0
    public var minValue: Double = Double.MIN_VALUE
    public var maxValue: Double = Double.MAX_VALUE
    public val filters: MutableSet<MetaEntryFilter<T, R>> = mutableSetOf()

    public fun value(newValue: Double): B {

        initialValue = newValue

        return this as B
    }

    public fun maximum(newValue: Double): B {

        maxValue = newValue

        return this as B
    }

    public fun minimum(newValue: Double): B {

        minValue = newValue

        return this as B
    }

    public fun identifier(identifier: String): B {

        this.identifier = identifier

        return this as B
    }

    public fun id(identifier: String): B {
        return identifier(identifier)
    }

    public fun rollable(newRollable: Rollable<T, R>): B {

        rollable = newRollable

        return this as B
    }


    public fun rollable(item: R): B {
        return rollable(Rollable.Single(item))
    }


    public fun rollable(block: () -> R): B {
        return rollable(Rollable.SingleByFun(block))
    }

    public fun addFilter(filter: MetaEntryFilter<T, R>): B {

        filters.add(filter)

        return this as B
    }


    public fun addFilter(block: MetaEntryFilterBuilder<T, R>.() -> Unit): B {
        return addFilter(MetaEntryFilterBuilder<T, R>().apply(block).build())
    }

    public abstract fun build(): E
}