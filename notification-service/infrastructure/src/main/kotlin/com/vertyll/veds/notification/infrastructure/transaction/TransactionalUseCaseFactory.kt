package com.vertyll.veds.notification.infrastructure.transaction

import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionNotification
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy

@Component
internal class TransactionalUseCaseFactory(
    transactionManager: PlatformTransactionManager,
) {
    private val readWrite = TransactionNotification(transactionManager)

    private val readOnly = TransactionNotification(transactionManager).apply { isReadOnly = true }

    fun <T : Any> wrap(
        contract: Class<T>,
        target: T,
        isReadOnly: (String) -> Boolean,
    ): T {
        val proxy =
            Proxy.newProxyInstance(contract.classLoader, arrayOf(contract)) { _, method, args ->
                val notification = if (isReadOnly(method.name)) readOnly else readWrite
                notification.execute {
                    try {
                        method.invoke(target, *(args ?: emptyArray()))
                    } catch (e: InvocationTargetException) {
                        throw e.targetException
                    }
                }
            }

        @Suppress("UNCHECKED_CAST")
        return proxy as T
    }
}
