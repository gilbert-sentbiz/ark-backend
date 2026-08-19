package io.kotest.provided

import com.sentbe.bizplatform.arc.support.SpringKotest6Extension
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.extensions.Extension

class ProjectConfig : AbstractProjectConfig() {
	override val extensions: List<Extension> = listOf(SpringKotest6Extension())
}
