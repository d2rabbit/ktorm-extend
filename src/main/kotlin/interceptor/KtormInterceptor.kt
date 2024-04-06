package org.noear.interceptor

import org.noear.DefaultTransactionManager
import org.noear.KtormTransactionManager
import org.noear.NewTransactionManager
import org.noear.annotation.KTran
import org.noear.logger
import org.noear.solon.core.aspect.Interceptor
import org.noear.solon.core.aspect.Invocation
import java.util.logging.Level
import java.util.logging.Logger


/**
 * [KTran]的埋点拦截器
 * @author kelthas
 * @since 2024/03/17
 */
open class KtormInterceptor : Interceptor {
    /**
     * 定义新的拦截器，以实现[KTran]的埋点获取
     *
     * @param inv
     */
    override fun doIntercept(inv: Invocation): Any? {
        val kTran = kTran(inv)
        val ktormManagers = KtormManagers(kTran.transactionType)
        return ktormManagers.ktormTransactionManager(kTran) { inv.invoke() }
    }

    private fun kTran(inv: Invocation): KTran = runCatching {
        inv.method().getAnnotation(KTran::class.java)?:inv.targetClz.getAnnotation(KTran::class.java)
    }.onFailure {
        logger.severe("kTran exception $it")
        it.printStackTrace()
    }.getOrDefault(KTran())

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
 *  根据每个[KTran]中的事务管理器类型，创建事务管理，并完成事务管理
 * @constructor
 * TODO
 *
 * @param ktormTransactionType
 */
class KtormManagers(ktormTransactionType: KtormTransactionType) {

    // 获取具体的事务管理
    private val transactionManager: KtormTransactionManager = ktormTransactionType.getTransactionManager()

    fun ktormTransactionManager(kTran: KTran, func: () -> Any?): Any? = kotlin.runCatching{
        var result: Any? = null;
        transactionManager.transactionManager(kTran.dataBaseName, kTran.isolation) {
            result = func()
        }

        result
    }.onFailure {
        logger.log(Level.SEVERE,"transactionManager exception $it")
        it.printStackTrace()
    }.getOrNull()
}