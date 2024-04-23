package com.d2rabbit.extensions


import com.d2rabbit.annotation.Alpha
import com.d2rabbit.annotation.Stable
import com.d2rabbit.extensions.KtormColumnSelection.multipleColumns
import com.d2rabbit.extensions.KtormColumnSelection.singleColumn
import com.d2rabbit.extensions.KtormExtendFun.page
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.entity.EntitySequence
import org.ktorm.entity.filter
import org.ktorm.entity.isEmpty
import org.ktorm.entity.isNotEmpty
import org.ktorm.schema.BaseTable
import org.ktorm.schema.ColumnDeclaring


object KtormExtendFun {
    /**
     * 存在判断的扩展函数，利用的序列接口里面的
     */
    @Stable
    public inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.exist(
        predicate: (T) -> ColumnDeclaring<Boolean>
    ): Boolean {
        return this.filter(predicate).isNotEmpty()
    }

    /**
     *
     * 不存在判定 简化查询方式
     */
    @Stable
    public inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.noExist(
        predicate: (T) -> ColumnDeclaring<Boolean>
    ): Boolean {
        return this.filter(predicate).isEmpty()
    }


    /**
     *  EntitySequence的分页查询，最终返回仍然是一个EntitySequence
     *
     */
    @Stable
    public fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.page(page: Int, pageSiz: Int): EntitySequence<E, T> {
        return this.withExpression(expression.copy(limit = pageSiz, offset = page * pageSiz))
    }

    /**
     *  EntitySequence的分页查询，最终返回仍然是一个EntitySequence
     *  此函数传入的[page]会直接进行减一操作
     *
     */
    public fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.page0(page: Int, pageSiz: Int): EntitySequence<E, T> {
        return this.withExpression(expression.copy(limit = pageSiz, offset = (page - 1) * pageSiz))
    }


    /**
     * 返回Query的分页，返回Query
     */
    @Stable
    public fun Query.page(page: Int, pageSiz: Long): Query {
        return this.limit((pageSiz * page).toInt(), pageSiz.toInt())
    }

    /**
     * 返回Query的分页，返回Query
     * 此函数传入的[page]会直接进行减一操作
     */
    @Stable
    public fun Query.page0(page: Int, pageSiz: Long): Query {
        return this.limit((pageSiz * page - 1).toInt(), pageSiz.toInt())
    }


    /**
     * 单表查询（后面增加一个联表表查询），返回一个Query
     */
    @Alpha
    fun <T : BaseTable<*>> Database.selectFromTable(
        table: T,
        block: (QueryColumnWithCondition<T>.(T) -> Unit)? = null
    ): Query {
        val querySource = this.from(table)
        if (block == null) {
            return querySource.select()
        }
        val queryColumWithCondition = QueryColumnWithCondition(table).apply {
            block(table)
        }
        val condition = queryColumWithCondition.condition
        val columnSelection = queryColumWithCondition.columnSelection
        if (condition == null) {
            return when (columnSelection) {
                is KtormColumnSelection.SingleColumn -> querySource.select(columnSelection.column)
                is KtormColumnSelection.MultipleColumns -> querySource.select(columnSelection.columns)
                null -> TODO()
            }
        }
        return when (columnSelection) {
            is KtormColumnSelection.SingleColumn -> querySource.select(columnSelection.column).condition(table)
            is KtormColumnSelection.MultipleColumns -> querySource.select(columnSelection.columns).condition(table)
            null -> TODO()
        }

    }


}


/**
 * 用于对[selectFromTable]的使用查询列目和查询条件的函数式整合
 */
@Alpha
object KtormColumnSelection {
    /**
     * 查询列密封基础接口
     */
    sealed interface ColumnSelection

    /**
     * 单列查询
     */
    data class SingleColumn internal constructor(val column: ColumnDeclaring<*>) : ColumnSelection

    /**
     * 多列查询
     */
    data class MultipleColumns internal constructor(val columns: Collection<ColumnDeclaring<*>>) : ColumnSelection

    //    公共创建函数
    public fun singleColumn(column: ColumnDeclaring<*>): SingleColumn = SingleColumn(column)

    //  公共创建函数
    public fun multipleColumns(vararg column: ColumnDeclaring<*>): MultipleColumns = MultipleColumns(column.asList())

}

/**
 * 查询内嵌函数类，用于对[selectFromTable]的使用查询列目和查询条件的函数式整合
 */
@Alpha
class QueryColumnWithCondition<T : BaseTable<*>>(var table: T) {
    var columnSelection: KtormColumnSelection.ColumnSelection? = null
    var condition: (Query.(T) -> Query)? = null

    /**
     * 内嵌查询函数
     */
    fun query(vararg columns: ColumnDeclaring<*>, condition: (Query.(T) -> Query)?) {
        this.condition = condition
        columnSelection = when (columns.size) {
            1 -> singleColumn(columns[0])
            else -> multipleColumns(*columns)
        }
    }

}


