package dtx.table

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.Rollable

public abstract class AbstractTableBuilder<
    Target,
    Rolled,
    TableType: Table<Target, Rolled>,
    EntryType: Rollable<Target, Rolled>,
    //holy guacamole generics batman
    BuilderType: AbstractTableBuilder<Target, Rolled, TableType, EntryType, BuilderType>
> {

    public var tableName: String = "Unnamed Table"
    public var getDropRateFunc: (Target) -> Double = { 1.0 }
    public var getRollModFunc: (Double) -> Double = { it }
    public var rollFunc: (Target, ArgMap) -> RollResult<Rolled> = { _, _ -> RollResult.Nothing() }
    public var onSelectFunc: (Target, RollResult<Rolled>) -> Unit = { _, _ -> }
    protected abstract val entries: MutableCollection<EntryType>

    public open fun name(name: String): BuilderType {
        this.tableName = name
        return this as BuilderType
    }

    public open fun addEntry(entry: EntryType): BuilderType {
        this.entries.add(entry)
        return this as BuilderType
    }

    public open fun getBaseDropRate(newBaseDropRate: (Target) -> Double): BuilderType {
        this.getDropRateFunc = newBaseDropRate
        return this as BuilderType
    }

    public open fun getRollModifier(newRollModifier: (Double) -> Double): BuilderType {
        this.getRollModFunc = newRollModifier
        return this as BuilderType
    }

    public open fun roll(newRoll: (Target, ArgMap) -> RollResult<Rolled>): BuilderType {
        this.rollFunc = newRoll
        return this as BuilderType
    }

    public open fun onSelect(newOnSelect: (Target, RollResult<Rolled>) -> Unit): BuilderType {
        this.onSelectFunc = newOnSelect
        return this as BuilderType
    }

    public abstract fun build(): TableType
}
