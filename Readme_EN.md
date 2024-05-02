# Ktorm Extended Component Library

## Background

Ktorm is a native ORM framework for Kotlin, and its latest version is currently 3.6.0. This extension plugin expands upon Ktorm's capabilities by adding support for transaction management with the Solon framework, as well as enhancing Ktorm's paging query functions and providing a simple functional abstraction for Ktorm's transaction functions.
>In fact, the primary motivation behind creating this extension plugin was to encapsulate some commonly used functions during my development process.

# 1. [Solon](https://solon.noear.org/) Transaction Support

>Solon is a Java "ecosystem" application development framework built from scratch, featuring its own standards and an open ecosystem. It has been certified by the OpenAtom Foundation and is now a incubator project under OpenAtom.
>The author, [noear (Westwind)](https://github.com/noear), is a prolific open-source contributor.

While Ktorm inherently supports transaction management through Spring, it lacks support for frameworks with their own transaction management systems. Therefore, this extension plugin introduces transaction support for the Solon framework. Given Ktorm's extensibility for transactions, theoretically, it could adopt a similar delegation approach to support transactions natively across various "ecosystem" frameworks.
This extension emulates the transactional behavior of Spring, adding Solon framework transaction support.

You can achieve Solon transaction delegation using the following function:

```kotlin
Database.connectWithSolonSupport(dataSource)
```

>After enabling Solon delegation, you must use the `@Tran` annotation for transaction usage, and this function is only compatible with Solon versions 2.7.4 and above.
>
>For versions below 2.7.4, please use Ktorm's default connection creation method. If you wish to manage transactions using annotations, the `@SolonTransaction` can be utilized, which is implemented through Solon's AOP features. For specifics, please refer to
> ***KtormInterceptor.kt***.
>
>For details, see ***SolonTransactionManager.kt***.

# 2. Transaction Wrapping Function

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

The transaction wrapping function is implemented by passing a `Database` object and specifying the transaction type (`transactionType`) and transaction isolation level. By default, it will use the current transaction; however, if `transactionType` is set to `NEW`, it will create a new transaction to execute the provided function.

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
>Note: The `DataBase` parameter is mandatory because the underlying implementation still relies on Ktorm's transaction functions, albeit with an upper-level abstraction. Thus, a `DataBase` object must be provided.
>
>Please refer to ***KtranFunction.kt*** for more information.
>
>The wrapped transaction function and `@SolonTransaction` share the same underlying implementation.

# 3. Ktorm Extended Operation Functions

* Data Existence Check Functions
```kotlin
    // Existence check
    fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.exist(
        predicate: (T) -> ColumnDeclaring<Boolean>
    ): Boolean {
        return this.filter(predicate).isNotEmpty()
    }
    // Non-existence check
    fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.noExist(
        predicate: (T) -> ColumnDeclaring<Boolean>
    ): Boolean {
        return this.filter(predicate).isEmpty()
    }
```

* Paging Query Functions
```kotlin
   /**
     * Paginates an EntitySequence, returning an EntitySequence
     */

    fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.page(page: Int, pageSiz: Int): EntitySequence<E, T> {
        return this.withExpression(expression.copy(limit = pageSiz, offset = page * pageSiz))
    }

    /**
     * Paginates an EntitySequence, returning an EntitySequence
     * The passed [page] value is decremented by one for calculation
     */
    fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.page0(page: Int, pageSiz: Int): EntitySequence<E, T> {
        return this.withExpression(expression.copy(limit = pageSiz, offset = (page - 1) * pageSiz))
    }

    /**
     * Returns a paged Query, returning a Query
     */

    fun Query.page(page: Int, pageSiz: Long): Query {
        return this.limit((pageSiz * page).toInt(), pageSiz.toInt())
    }

    /**
     * Returns a paged Query, returning a Query
     * The passed [page] value is decremented by one for calculation
     */
    fun Query.page0(page: Int, pageSiz: Long): Query {
        return this.limit((pageSiz * page - 1).toInt(), pageSiz.toInt())
    }
```

# 4. Coordinates

* Maven
```xml
<dependency>
    <groupId>com.d2rabbit</groupId>
    <artifactId>ktorm-extend</artifactId>
    <version>0.0.2-alpha</version>
</dependency>
```

* Gradle (Kotlin)
```kotlin
implementation("com.d2rabbit:ktorm-extend:0.0.2-alpha")
```

* Gradle
```groovy
implementation group: 'com.d2rabbit', name: 'ktorm-extend', version: '0.0.2-alpha'
implementation 'com.d2rabbit:ktorm-extend:0.0.2-alpha'
```