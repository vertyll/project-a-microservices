package com.vertyll.veds.project.infrastructure.transaction

import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy

@Component
internal class TransactionalUseCaseFactory(
    transactionManager: PlatformTransactionManager,
) {
    private val readWrite = TransactionTemplate(transactionManager)

    private val readOnly = TransactionTemplate(transactionManager).apply { isReadOnly = true }

    inline fun <reified T : Any> wrap(
        target: T,
        readOnlyMethods: Set<String>,
    ): T = wrap(T::class.java, target, readOnlyMethods)

    fun <T : Any> wrap(
        contract: Class<T>,
        target: T,
        readOnlyMethods: Set<String>,
    ): T {
        val declared = contract.methods.map { it.name }.toSet()
        val unknown = readOnlyMethods - declared
        check(unknown.isEmpty()) {
            "read-only methods not declared on ${contract.simpleName}: ${unknown.joinToString()}"
        }

        val proxy =
            Proxy.newProxyInstance(contract.classLoader, arrayOf(contract)) { _, method, args ->
                val template = if (method.name in readOnlyMethods) readOnly else readWrite
                template.execute {
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
