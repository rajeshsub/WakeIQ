package com.wakeiq

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.ExternalResource

class GrantSystemPermissionsRule : ExternalResource() {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val pkg = instrumentation.targetContext.packageName

    override fun before() {
        val auto = instrumentation.uiAutomation
        auto.executeShellCommand("pm grant $pkg android.permission.POST_NOTIFICATIONS").close()
        auto.executeShellCommand("dumpsys deviceidle whitelist +$pkg").close()
        auto.executeShellCommand("cmd appops set $pkg USE_FULL_SCREEN_INTENT allow").close()
        Thread.sleep(300)
    }
}
