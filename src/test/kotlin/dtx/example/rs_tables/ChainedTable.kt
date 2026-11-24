package dtx.example.rs_tables

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.impl.chain.*

open class RSChainedTable<T, R>(
    private val inner: ChainedTable<T, R>
): RSTable<T, R>, ChainedTable<T, R>, ChainedTableHooks<T, R> by inner {

    override val tableIdentifier: String = inner.tableIdentifier
    override val head: ChainRollable<T, R> = inner.head
    override val tableEntries: Collection<ChainRollable<T, R>> = inner.tableEntries

    override fun selectResult(target: T, otherArgs: ArgMap): RollResult<R> = inner.selectResult(target, otherArgs)

    override fun toString(): String = inner.toString()

    companion object {
        val EmptyTable: RSChainedTable<Any?, Any?> = RSChainedTable(
            ChainedTableImpl("", ChainEnd(), null, ChainedTableHooks.Default())
        )
        @Suppress("UNCHECKED_CAST")
        fun <T, R> Empty(): RSChainedTable<T, R> = EmptyTable as RSChainedTable<T, R>
    }
}

open class RSChainedTableBuilder<T, R>: ChainedTableBuilder<T, R, RSChainedTable<T, R>>(
    impl = { name, head, defaultRoll, hooks ->
        val inner = ChainedTableImpl(name, head, defaultRoll, hooks)
        RSChainedTable(inner)
    }
)

fun <T, R> rsChainedTable(block: RSChainedTableBuilder<T, R>.() -> Unit): RSChainedTable<T, R> {
    val builder = RSChainedTableBuilder<T, R>()
    builder.apply(block)
    return builder.build()
}
