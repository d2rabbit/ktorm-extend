package com.d2rabbit.solon

import org.noear.solon.exception.SolonException

internal data class SolonTransactionException(override val cause: Throwable?) : SolonException(cause)