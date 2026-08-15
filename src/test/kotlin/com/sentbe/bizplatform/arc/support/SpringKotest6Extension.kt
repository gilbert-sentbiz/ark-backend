package com.sentbe.bizplatform.arc.support

import io.kotest.core.extensions.SpecExtension
import io.kotest.core.spec.Spec
import org.springframework.test.context.TestContextManager

class SpringKotest6Extension : SpecExtension {
    override suspend fun intercept(
        spec: Spec,
        execute: suspend (Spec) -> Unit,
    ) {
        val manager = TestContextManager(spec::class.java)
        manager.prepareTestInstance(spec)
        execute(spec)
    }
}
