# &#x20;专为solon框架适配的solon事务管理插件

## 背景

由于solon自身的事务框架对于ktorm目前的支持存在一定的问题，所以我采取了另外的一种方式，利用solon特有的精简化的AOP特性以及注解继承的特性，单独实现了一个ktorm的事务委托方式。

事实上ktorm自身的事务是由原生的`JDBC`实现的，而且ktorm本身的事务是由一个抽象的事务管理接口[\`TransactionManager\`](https://github.com/kotlin-orm/ktorm/blob/master/ktorm-core/src/main/kotlin/org/ktorm/database/TransactionManager.kt)来进行管理的，其中ktorm本身对于spring系列的事务支持就是通过改方式实现的，不过我也尝试过模仿ktorm对于spring的事务支持的方式仿写过solon的实现，但是会出现`connect is closed`的问题，这部分问题，我并没有定位到具体的问题，只能无脑的认为是二者在链接管理的方式上存在一些问题，但是参考spring的事务委托方式solon对于ktorm的事务委托是生效的，不过由于`connect is closed`异常，会导致所有的提交操作全部回滚，所以理论上参考spring的实现方式是没问题的，只不过需要解决上述的问题。下图是ktorm的事务委托给spring的方式。（注意：图来自于ktorm的[官网](https://www.ktorm.org/zh-cn/spring-support.html#%E4%BA%8B%E5%8A%A1%E4%BB%A3%E7%90%86)）

![image.png](http://cloud.d2rabbit.com/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20240406145840.png)

## &#x20;ktorm for solon插件的实现方式：

### 运用的solon和ktorm的特性

1.  **solon的[AOP](https://solon.noear.org/article/35)特性和[注解继承](https://solon.noear.org/article/619)的特性**

    使用注解`@KTran`作为事务的埋点，然后利用`@Around`注解实现了埋点接入拦截器

    `埋点注解`

    ```kotlin
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
    ```

    `埋点拦截器`

    ```kotlin
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
    }
    ```

2.  [ktorm原生的事务管理器](https://www.ktorm.org/zh-cn/transaction-management.html)

    `ktorm原生事务管理器的二次封装`

    ```kotlin
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
    ```

    上述中通过接收声明的`Database`对象和`TransactionIsolation`事务级别对象，以及函数体本身简化了原有的事务声明方式

    **注意，上述的事务声明封装函数主要为函数体内插件内部使用，但是本质上也支持业务集成使用**

3.  插件直接集成`DataBase`对象的获取

    ```kotlin
    open class KtormDelegate<T>(private val dateBaseName:String?=null) {
        operator fun <T> getValue(thisRef: Any?, property: KProperty<*>): T {
           val dataBase =  if (!dateBaseName.isNullOrBlank()){
                Solon.context().getBean(dateBaseName.toString())
            }else{
                Solon.context().getBean(Database::class.java)
            }
            return dataBase as T
        }
    }


    data object DefaultTransactionManager:KtormTransactionManager{
        override fun transactionManager(dataBaseName:String,isolation: TransactionIsolation?, func: () -> Unit) {
            logger.info("DefaultTransactionManager execute")
            val database:Database by KtormDelegate<Database>(dataBaseName)
            currentTransactionManager(database,isolation){
                func()
            }
        }
    }   
    ```

    可直接通过` val database:Database by KtormDelegate<Database>(dataBaseName)`的方式获取到`DataBase`的对象

    **注意：此处dataBaseName是为了应对多个数据源注入到solon bean里面实现的，默认dataBaseName可以是空的，具体请参考[solon通过上下文获取bean的方式](https://solon.noear.org/article/33)**

## 插件的使用方式

*   支持`@KTran`注解类级别加载
*   支持`@KTran`注解函数级别加载
*   支持`nextTransactionManager`函数式使用，此函数的使用方式等同于

    &#x20; `@KTran(transactionType = KtormTransactionType.NEW_TRANSACTION_MANAGER )`
*   支持`defaultTransactionManager`函数式使用，此函数的使用方式等同于

    `@KTran(transactionType = KtormTransactionType.DEFAULT_TRANSACTION_MANAGER)`

具体使用方法可参考下面的示例代码：

```kotlin
        @KTran(transactionType = KtormTransactionType.NEW_TRANSACTION_MANAGER )
        @Component
        open class DemoService {

            private val database:Database by KtormDelegate<Database>()

            @KTran
            open fun list() {
                val res = database.departments.toList()
                println(res)
            }


            open fun insert(){
                val department = Department{
                    this.departmentNumber="1113"
                    this.name="异常"
                    this.parentId=0
                }
                database.departments.add(department)
                throw RuntimeException("测试异常")
            }


            open fun insert2(){
                nextTransactionManager(database){
                    val department = Department{
                        this.departmentNumber="111"
                        this.name="哦OK"
                        this.parentId=0
                    }
                    database.departments.add(department)
                    throw RuntimeException("测试异常")
                }

            }
            open fun insert3(){
                defaultTransactionManager(database) {
                    val department = Department {
                        this.departmentNumber = "111"
                        this.name = "哦OK"
                        this.parentId = 0
                    }
                    database.departments.add(department)
                    throw RuntimeException("测试异常")
                }

            }

        }

```


## 插件坐标

    ```
    <dependency>
        <groupId>com.d2rabbit</groupId>
        <artifactId>ktorm-solon-plugin</artifactId>
        <version>0.0.1</version>
    </dependency>
    ```