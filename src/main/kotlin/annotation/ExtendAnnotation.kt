package com.d2rabbit.annotation

/**
 * 下方所有注解仅适用于本扩展框架
 */

/**
 *
 * 符号版本等级标记，仅作标记使用，不具有实际功能，Alpha
 * 此标记所标记的函数或类，在未来版本可能会发生更改
 */
@Target(AnnotationTarget.CLASS,AnnotationTarget.FUNCTION,AnnotationTarget.ANNOTATION_CLASS)
@Retention
@ExperimentalSubclassOptIn
annotation class Alpha

/**
 *
 * 符号版本等级标记，仅作标记使用，不具有实际功能，Beta
 * 此标记所标记的函数或类，基本逻辑已确定，但是未经过稳定性测试，仅经过功能测试
 */
@Target(AnnotationTarget.CLASS,AnnotationTarget.FUNCTION,AnnotationTarget.ANNOTATION_CLASS)
@Retention
@ExperimentalSubclassOptIn
annotation class Beta


/**
 *
 * 符号版本等级标记，仅作标记使用，不具有实际功能，Stable
 * 此标记所标记的函数或类，已经可以正常使用
 */
@Target(AnnotationTarget.CLASS,AnnotationTarget.FUNCTION,AnnotationTarget.ANNOTATION_CLASS)
@Retention
@ExperimentalSubclassOptIn
annotation class Stable