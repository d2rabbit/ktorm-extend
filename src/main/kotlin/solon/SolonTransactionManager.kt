package com.d2rabbit.solon

import org.ktorm.database.*
import org.ktorm.logging.Logger
import org.ktorm.logging.detectLoggerImplementation
import org.noear.solon.Solon
import org.noear.solon.data.tran.TranUtils
import org.noear.solon.exception.SolonException
import java.sql.Connection
import javax.sql.DataSource

/**
 * [TransactionManager] 将所有事务委托给 solon 框架的实现。
 * 此类启用 solon 支持，并由函数创建[Database.connectWithSolonSupport]的实例使用它[Database]。一旦启用了 solon 支持，事务管理将被委托给solon框架，因此该[Database.useTransaction]功能不再可用，应用程序应改用solon的事务注释。
 * ps：此部分参考自ktorm对于spring的支持[SpringManagedTransactionManager]
 * @property dataSource
 */

internal class SolonTransactionManager(private val dataSource: DataSource) : TransactionManager {
    override val currentTransaction: Transaction?
        get() = null
    override val defaultIsolation: TransactionIsolation?
        get() = null

    /**
     * TODO
     * 仅支持 solon 2.7.4+版本
     * @return
     */
    override fun newConnection(): Connection {

        return TranUtils.getConnectionProxy(dataSource)
    }

    override fun newTransaction(isolation: TransactionIsolation?): Transaction {
        val msg = "Transaction is managed by Solon, please use Solon's @Tran annotation instead."
        throw UnsupportedOperationException(msg)
    }
}

/**
 * TODO
 * 参考[Database.connectWithSpringSupport]
 * @param dataSource
 * @param dialect
 * @param logger
 * @param alwaysQuoteIdentifiers
 * @param generateSqlInUpperCase
 * @return
 */
public fun Database.Companion.connectWithSolonSupport(
    dataSource: DataSource,
    dialect: SqlDialect = detectDialectImplementation(),
    logger: Logger = detectLoggerImplementation(),
    alwaysQuoteIdentifiers: Boolean = false,
    generateSqlInUpperCase: Boolean? = null
): Database {
    val version = Solon.version()
    if (version < "2.7.4") throw UnsupportedOperationException("Solon version must be 2.7.4+,please use default connection.")
    return Database(
        transactionManager = SolonTransactionManager(dataSource),
        dialect = dialect,
        logger = logger,
        exceptionTranslator = { ex -> SolonTransactionException(ex) },
        alwaysQuoteIdentifiers = alwaysQuoteIdentifiers,
        generateSqlInUpperCase = generateSqlInUpperCase
    )
}
