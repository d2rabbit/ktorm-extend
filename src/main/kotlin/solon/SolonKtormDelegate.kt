package com.d2rabbit.solon

import com.d2rabbit.annotation.Beta
import org.ktorm.database.Database
import org.noear.solon.Solon
import kotlin.reflect.KProperty

/**
 * dataBase 委托获取，发挥kotlin的特性，方便快速获取dataBase的bean，但是仅适用于solon框架
 * @author kelthas
 * @since 2024/03/17
 */
@Beta
open class SolonKtormDelegate(private val dateBaseName:String?=null) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Database {
       val dataBase =  if (!dateBaseName.isNullOrBlank()){
            Solon.context().getBean(dateBaseName.toString())
        }else{
            Solon.context().getBean(Database::class.java)
        }
        return dataBase
    }
}


/**
 * dataBase 函数获取，方便快速获取dataBase的bean，但是仅适用于solon框架
 * @author kelthas
 * @since 2024/03/17
 */
@Beta
 fun  ktorm(dateBaseName:String?=null):SolonKtormDelegate{
     return SolonKtormDelegate(dateBaseName)
 }



/**
 * dataBase 委托获取，获取默认的委托，仅适用于solon框架
 * @author kelthas
 * @since 2024/03/17
 */
val solonDatabase get() = ktorm()
