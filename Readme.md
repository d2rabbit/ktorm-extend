# &#x20;Ktorm 扩展化组件库

## 背景

Ktorm是Kotlin的原生ORM框架，目前最新版本为3.6.0，本扩展插件在Ktorm的基础上进行简单扩展，添加了对Solon框架的事务支持，同时支持了Ktorm的分页查询函数和对Ktorm的事务函数做了简单的函数封装。
>其实主要是我自己在开发过程中想要封装一些常用的函数，所以就写了这个扩展插件。
# 1.[Solon](https://solon.noear.org/)事务支持

> Solon是一个Java “生态型”应用开发框架。从零开始构建，有自主的标准规范与开放生态。目前已经通过了开放原子基金会的认证，成为了开放原子基金会的孵化项目。
> 作者 [noear（西东）](https://github.com/noear)是一位多产型开源作者。

Ktorm本身支持Spring的事务托管，但是缺乏对于第三方的具有本身事务管理体系的框架的支持，所以本扩展插件添加了Solon框架的事务支持。由于Ktorm本身提供了事务扩展的可能性，所以理论上可以参考Spring的事务委托方式实现对于所有“生态型”框架的原生支持。
本扩展仿照Spring的事务方式，添加了Solon框架的事务支持。

使用以下函数可以实现Solon的事务委托。

```kotlin
Database.connectWithSolonSupport(dataSource)
```

> 启用Solon委托之后，就只能使用`@Tran`注解来使用事务了,并且该函数仅支持Solon
> 2.7.4+的版本。
>
>2.7.4以下的版本请使用Ktorm默认的连接创建方式，如果想要使用注解是事务管理，可以使用`@SolonTransaction`来实现，
> 该注解是通过Solon的AOP特性来实现的，具体请参考
> ***KtormInterceptor.kt***。
>
> 具体实现请参考如 ***SolonTransactionManager.kt***。

# 2.事务封装函数

```kotlin
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
```

通过传递Database和事务类型`transactionType`以及事务隔离级别来实现的，默认情况下会使用当前的事务，如果事务类型声明为`NEW`，则会创建一个新的事务来执行。

```kotlin
 fun insert() {
    transaction(database) {
        val department = Department {
            this.departmentNumber = "1"
            this.name = "开发部"
            this.parentId = 0
        }
        database.departments.add(department)
    }
}
```
>注意：DataBase参数是必须传递的，因为封装的底层仍然是通过Ktorm的事务函数来实现的，仅仅是做了上层封装，所以需要传递一个DataBase对象。
> 请参考***KtranFunction.kt***。
> 
> 封装的事务函数和`@SolonTransaction`的底层实现是想通的。

# 3.Ktorm的扩展操作函数

* 数据存在判断函数
```kotlin
    //存在判断
    fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.exist(
        predicate: (T) -> ColumnDeclaring<Boolean>
    ): Boolean {
        return this.filter(predicate).isNotEmpty()
    }
    //不存在判断
    fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.noExist(
      predicate: (T) -> ColumnDeclaring<Boolean>
    ): Boolean {
      return this.filter(predicate).isEmpty()
    }
```    

* 分页查询函数
```kotlin
   /**
     *  EntitySequence的分页查询，最终返回仍然是一个EntitySequence
     *
     */

    fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.page(page: Int, pageSiz: Int): EntitySequence<E, T> {
        return this.withExpression(expression.copy(limit = pageSiz, offset = page * pageSiz))
    }

    /**
     *  EntitySequence的分页查询，最终返回仍然是一个EntitySequence
     *  此函数传入的[page]会直接进行减一操作
     *
     */
    fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.page0(page: Int, pageSiz: Int): EntitySequence<E, T> {
        return this.withExpression(expression.copy(limit = pageSiz, offset = (page - 1) * pageSiz))
    }


    /**
     * 返回Query的分页，返回Query
     */

     fun Query.page(page: Int, pageSiz: Long): Query {
        return this.limit((pageSiz * page).toInt(), pageSiz.toInt())
    }

    /**
     * 返回Query的分页，返回Query
     * 此函数传入的[page]会直接进行减一操作
     */
    fun Query.page0(page: Int, pageSiz: Long): Query {
        return this.limit((pageSiz * page - 1).toInt(), pageSiz.toInt())
    }

```