package org.noear

import org.ktorm.database.Database
import org.ktorm.database.TransactionIsolation
import org.ktorm.database.TransactionManager
import org.noear.KtranFunction.currentTransactionManager
import org.noear.KtranFunction.newTransactionManager
import org.noear.delegation.KtormDelegate
import java.util.logging.Logger

/**
 * ktrom 事务函数二次封装，方便调用
 * @author kelthas
 * @since 2024/03/17
 */

 val logger: Logger = Logger.getLogger("ktorm-solon-plugin")

object KtranFunction {


    /**
     * 当前事务 默认事务函数的封装  默认事务级别 [TransactionIsolation.READ_COMMITTED]
     * @author kelthas
     * @param database
     * @param func
     * @since 2024/03/17
     */
    fun currentTransactionManager(database: Database,isolation : TransactionIsolation? =null ,func:()->Unit){
         database.useTransaction(isolation){
            func()
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
    fun newTransactionManager(transactionManager: TransactionManager, isolation : TransactionIsolation? = TransactionIsolation.READ_COMMITTED, func:()->Unit)  {
        val transaction = transactionManager.newTransaction(isolation)
        runCatching {
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
sealed interface KtormTransactionManager{
    fun transactionManager(dataBaseName:String,isolation: TransactionIsolation?, func: () -> Unit)
}



/**
 * 默认事务管理器
 */
data object DefaultTransactionManager:KtormTransactionManager{
    override fun transactionManager(dataBaseName:String,isolation: TransactionIsolation?, func: () -> Unit) {
        logger.info("DefaultTransactionManager execute")
        val database:Database by KtormDelegate<Database>(dataBaseName)
        currentTransactionManager(database,isolation){
            func()
        }
    }
}
/**
 * 新 事务管理器
 */
data object NewTransactionManager:KtormTransactionManager{
    override fun transactionManager(dataBaseName:String,isolation: TransactionIsolation?, func: () -> Unit) {
        val database:Database by KtormDelegate<Database>(dataBaseName)
        logger.info("NewTransactionManager execute")
        val newTransactionManager = database.transactionManager
        newTransactionManager(newTransactionManager,isolation,func)
    }

}


/**
 * TODO
 * 对于默认的dataBase的事务闭包函数的二次封装
 * @param database
 * @param isolation
 * @param func
 */
fun defaultTransactionManager(database: Database,isolation: TransactionIsolation?=null, func: () -> Unit){
    currentTransactionManager(database,isolation,func=func)
}
/**
 * TODO
 * 创建新的事务函数的直接调用的封装函数
 * @param database
 * @param isolation
 * @param func
 */
fun nextTransactionManager(database: Database, isolation: TransactionIsolation?=null, func: () -> Unit){
    newTransactionManager(database.transactionManager,isolation,func)
}