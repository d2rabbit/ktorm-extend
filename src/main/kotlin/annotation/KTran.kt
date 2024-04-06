package org.noear.annotation

import org.ktorm.database.TransactionIsolation
import org.noear.interceptor.KtormInterceptor
import org.noear.interceptor.KtormTransactionType
import org.noear.solon.annotation.Around
import java.lang.annotation.Inherited

/**
 * ktrom 事务注解，由于ktorm框架直接委托给solon自身的事务管理器存在链接异常关闭的问题，故此以[KTran]注解为指定埋点，方便使用
 * @author kelthas
 * @since 2024/03/17
 * @property isolation  默认为[TransactionIsolation.READ_COMMITTED]
 * @property transactionType  默认为[KtormTransactionType.DEFAULT_TRANSACTION_MANAGER]
 */
@Inherited
@Target(AnnotationTarget.CLASS,AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Around(KtormInterceptor::class)
annotation class KTran(

    val isolation :TransactionIsolation = TransactionIsolation.READ_COMMITTED,

    val transactionType: KtormTransactionType = KtormTransactionType.DEFAULT_TRANSACTION_MANAGER,

    val dataBaseName:String = ""

)
