package dtx.table

import dtx.core.BaseDroprate
import dtx.core.OnSelect
import dtx.core.Roll
import dtx.core.RollModifier
import dtx.core.RollResult
import dtx.core.Rollable
import dtx.core.ShouldRoll
import dtx.core.defaultShouldRoll

public abstract class AbstractTableBuilder<
    Target,
    Rolled,
    TableType: Table<Target, Rolled>,
    EntryType: Rollable<Target, Rolled>,
    //holy guacamole generics batman
    BuilderType: AbstractTableBuilder<Target, Rolled, TableType, EntryType, BuilderType>
> {

    public var tableName: String = "Unnamed Table"
    public var getDropRateFunc: BaseDroprate<Target> = Rollable.Companion::defaultGetBaseDropRate
    public var getRollModFunc: RollModifier<Target> = Table.Companion::defaultRollModifier
    public var rollFunc: Roll<Target, Rolled> = { _, _ -> RollResult.Nothing() }
    public var onSelectFunc: OnSelect<Target, Rolled> = Rollable.Companion::defaultOnSelect
    public var shouldRollFunc: ShouldRoll<Target> = ::defaultShouldRoll
    protected abstract val entries: MutableCollection<EntryType>

    public open fun name(name: String): BuilderType {

        this.tableName = name

        return this as BuilderType
    }

    public open fun addEntry(entry: EntryType): BuilderType {

        this.entries.add(entry)

        return this as BuilderType
    }

    public open fun getBaseDropRate(newBaseDropRate: BaseDroprate<Target>): BuilderType {

        this.getDropRateFunc = newBaseDropRate

        return this as BuilderType
    }

    public open fun modifyRoll(newRollModifier: RollModifier<Target>): BuilderType {

        this.getRollModFunc = newRollModifier

        return this as BuilderType
    }

    public open fun roll(newRoll: Roll<Target, Rolled>): BuilderType {

        this.rollFunc = newRoll

        return this as BuilderType
    }

    public open fun onSelect(newOnSelect: OnSelect<Target, Rolled>): BuilderType {

        this.onSelectFunc = newOnSelect

        return this as BuilderType
    }

    public open fun shouldRoll(newShouldRoll: ShouldRoll<Target>): BuilderType {

        this.shouldRollFunc = newShouldRoll

        return this as BuilderType
    }

    public abstract fun build(): TableType
}
