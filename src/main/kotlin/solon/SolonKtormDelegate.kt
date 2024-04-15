package com.d2rabbit.solon

import org.ktorm.database.Database
import org.noear.solon.Solon
import kotlin.reflect.KProperty

/**
 * dataBase 委托获取，发挥kotlin的特性，便于在非solon的组件类或者bean中获取到dataBase
 * @author kelthas
 * @since 2024/03/17
 */
open class SolonKtormDelegate<T>(private val dateBaseName:String?=null) {
    operator fun <T> getValue(thisRef: Any?, property: KProperty<*>): T {
       val dataBase =  if (!dateBaseName.isNullOrBlank()){
            Solon.context().getBean(dateBaseName.toString())
        }else{
            Solon.context().getBean(Database::class.java)
        }
        return dataBase as T
    }
}