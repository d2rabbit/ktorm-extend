package com.d2rabbit.interceptor

import com.d2rabbit.DefaultTransactionManager
import com.d2rabbit.KtormTransactionManager
import com.d2rabbit.NewTransactionManager
import com.d2rabbit.annotation.SolonTransaction
import com.d2rabbit.logger
import org.noear.solon.core.aspect.Interceptor
import org.noear.solon.core.aspect.Invocation
import java.util.logging.Level


/**
 * [SolonTransaction]的埋点拦截器
 * @author kelthas
 * @since 2024/03/17
 */
open class KtormInterceptor : Interceptor {
    /**
     * 定义新的拦截器，以实现[SolonTransaction]的埋点获取
     *
     * @param inv
     */
    override fun doIntercept(inv: Invocation): Any? {
        val kTran = kTran(inv)
        val ktormManagers = KtormManagers(kTran.transactionType)
        return ktormManagers.ktormTransactionManager(kTran) { inv.invoke() }
    }

    private fun kTran(inv: Invocation): SolonTransaction = runCatching {
        inv.method().getAnnotation(SolonTransaction::class.java)?:inv.targetClz.getAnnotation(SolonTransaction::class.java)
    }.onFailure {
        logger.severe("kTran exception $it")
        it.printStackTrace()
    }.getOrDefault(SolonTransaction())

}

enum class KtormTransactionType(private val transactionManager: KtormTransactionManager) {
    //采用默认事务管理 即当前事务管理
    DEFAULT_TRANSACTION_MANAGER(DefaultTransactionManager),

    //  创建新的事务管理
    NEW_TRANSACTION_MANAGER(NewTransactionManager),
    ;

    // 获取当前枚举值所代表的 transactionManager
    fun getTransactionManager(): KtormTransactionManager {
        return transactionManager
    }

}

/**
 * TODO
 *  根据每个[SolonTransaction]中的事务管理器类型，创建事务管理，并完成事务管理
 * @constructor
 * TODO
 *
 * @param ktormTransactionType
 */
internal class KtormManagers(ktormTransactionType: KtormTransactionType) {

    // 获取具体的事务管理
    private val transactionManager: KtormTransactionManager = ktormTransactionType.getTransactionManager()

    fun ktormTransactionManager(ktormTransaction: SolonTransaction, func: () -> Any?): Any? = kotlin.runCatching{
        var result: Any? = null;
        transactionManager.transactionManager(ktormTransaction.dataBaseName, ktormTransaction.isolation) {
            result = func()
        }

        result
    }.onFailure {
        logger.log(Level.SEVERE,"transactionManager exception $it")
        it.printStackTrace()
    }.getOrNull()
}