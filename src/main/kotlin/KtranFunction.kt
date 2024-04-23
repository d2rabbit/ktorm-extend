package com.d2rabbit

import org.ktorm.database.Database
import org.ktorm.database.TransactionIsolation
import org.ktorm.database.TransactionManager
import com.d2rabbit.KtranFunction.currentTransactionManager
import com.d2rabbit.KtranFunction.newTransactionManager
import com.d2rabbit.solon.SolonKtormDelegate
import java.util.logging.Logger
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.jvm.internal.impl.renderer.DescriptorRenderer.ValueParametersHandler.DEFAULT
import kotlin.reflect.jvm.reflect

/**
 * ktrom 事务函数二次封装，方便调用
 * @author kelthas
 * @since 2024/03/17
 */

val logger: Logger = Logger.getLogger("ktorm-solon-plugin")

/**
 * 新 事务类型等同于[KTran]的[KtormTransactionType.NEW_TRANSACTION_MANAGER]
 */
const val NEW = 0

/**
 * 当前 事务类型等同于[KTran]的[KtormTransactionType.DEFAULT_TRANSACTION_MANAGER]
 */
const val DEFAULT = 1


internal object KtranFunction {

    /**
     * 当前事务 默认事务函数的封装  默认事务级别 [TransactionIsolation.READ_COMMITTED]
     * @author kelthas
     * @param database
     * @param func
     * @since 2024/03/17
     */
    internal fun <R> currentTransactionManager(
        database: Database,
        isolation: TransactionIsolation? = null,
        func: (Database) -> R
    ): R {
        return database.useTransaction(isolation) {
            func(database)
        }
    }

    /**
     *  指定事务级别，创建新的事务的函数封装  默认事务级别 [TransactionIsolation.READ_COMMITTED]
     * @author kelthas
     * @param transactionManager
     * @param isolation
     * @param func
     * @since 2024/03/17
     */
    internal fun <R> newTransactionManager(
        transactionManager: TransactionManager,
        isolation: TransactionIsolation? = TransactionIsolation.READ_COMMITTED,
        func: () -> R
    ): R {
        val transaction = transactionManager.newTransaction(isolation)
        return runCatching {
            func()
        }.onSuccess {
            transaction.commit()
        }.onFailure {
            transaction.rollback()
        }.apply {
            transaction.close()
        }.getOrThrow()
    }
}

/**
 *
 * 事务管理接口
 */
sealed interface KtormTransactionManager {
    fun transactionManager(dataBaseName: String, isolation: TransactionIsolation?, func: () -> Unit)
}


/**
 * 默认事务管理器
 */
internal data object DefaultTransactionManager : KtormTransactionManager {
    override fun transactionManager(dataBaseName: String, isolation: TransactionIsolation?, func: () -> Unit) {
        logger.info("DefaultTransactionManager execute")
        val database: Database by SolonKtormDelegate(dataBaseName)
        currentTransactionManager(database, isolation) {
            func()
        }
    }
}

/**
 * 新 事务管理器
 */
internal data object NewTransactionManager : KtormTransactionManager {
    override fun transactionManager(dataBaseName: String, isolation: TransactionIsolation?, func: () -> Unit) {
        val database: Database by SolonKtormDelegate(dataBaseName)
        logger.info("NewTransactionManager execute")
        val newTransactionManager = database.transactionManager
        newTransactionManager(newTransactionManager, isolation, func)
    }

}


/**
 * TODO
 * 对于默认的dataBase的事务闭包函数的二次封装
 * @param database
 * @param isolation
 * @param func
 */
internal fun defaultTransactionManager(
    database: Database,
    isolation: TransactionIsolation? = null,
    func: (Database) -> Unit
) {
    currentTransactionManager(database, isolation, func = func)
}

/**
 * TODO
 * 创建新的事务函数的直接调用的封装函数
 * @param database
 * @param isolation
 * @param func
 */
internal fun <R> nextTransactionManager(
    transactionManager: TransactionManager,
    isolation: TransactionIsolation? = null,
    func: () -> R
): R {
    return newTransactionManager(transactionManager, isolation, func)
}

@OptIn(ExperimentalContracts::class)
fun <R> transaction(
    database: Database,
    transactionType: Int = DEFAULT,
    isolation: TransactionIsolation? = null,
    func: (Database) -> R
): R {
    contract {
        callsInPlace(func, InvocationKind.EXACTLY_ONCE)
    }
    return when (transactionType) {
        NEW -> nextTransactionManager(database.transactionManager, isolation) {
            func(database)
        }

        DEFAULT -> currentTransactionManager(database, isolation, func)
        else -> throw UnsupportedOperationException("The current transaction creation type is not supported")
    }

}