package dtx.impl

import dtx.core.ResultSelector
import dtx.core.Rollable
import dtx.core.SingleRollableBuilder

public abstract class AbstractMetaRollableBuilder<Target, Rolled, EntryType: MetaRollable<Target, Rolled>, BuilderType: AbstractMetaRollableBuilder<Target, Rolled, EntryType, BuilderType>> {

    public var identifier: String = ""
    public var rollable: Rollable<Target, Rolled> = Rollable.Empty()
    public var initialValue: Double = 1.0
    public var minValue: Double = Double.MIN_VALUE
    public var maxValue: Double = Double.MAX_VALUE
    public val filters: MutableSet<MetaEntryFilter<Target, Rolled>> = mutableSetOf()

    public fun value(newValue: Double): BuilderType {

        initialValue = newValue

        return this as BuilderType
    }

    public fun maximum(newValue: Double): BuilderType {

        maxValue = newValue

        return this as BuilderType
    }

    public fun minimum(newValue: Double): BuilderType {

        minValue = newValue

        return this as BuilderType
    }

    public fun identifier(identifier: String): BuilderType {

        this.identifier = identifier

        return this as BuilderType
    }

    public fun id(identifier: String): BuilderType {
        return identifier(identifier)
    }

    public fun rollable(newRollable: dtx.core.Rollable<Target, Rolled>): BuilderType {

        rollable = newRollable

        return this as BuilderType
    }

    public fun rollable(item: Rolled): BuilderType {
        return rollable(Rollable.Single(item))
    }

    public fun rollableBy(block: ResultSelector<Target, Rolled>): BuilderType {
        return rollable(Rollable.SingleByFun(block))
    }

    public fun rollable(block: SingleRollableBuilder<Target, Rolled>.() -> Unit): BuilderType {

        val built = SingleRollableBuilder<Target, Rolled>()
            .apply(block)
            .build()
        rollable(built)

        return this as BuilderType
    }

    public fun addFilter(filter: MetaEntryFilter<Target, Rolled>): BuilderType {

        filters.add(filter)

        return this as BuilderType
    }


    public fun addFilter(block: MetaEntryFilterBuilder<Target, Rolled>.() -> Unit): BuilderType {
        return addFilter(MetaEntryFilterBuilder<Target, Rolled>().apply(block).build())
    }

    public abstract fun build(): EntryType
}