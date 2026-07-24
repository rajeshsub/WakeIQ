plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.junit5.android) apply false
    alias(libs.plugins.kover) apply false
}

tasks.register("bootstrap") {
    group = "setup"
    description = "One-step dev setup: wires pre-commit (lint/format) and pre-push (tests) git hooks."

    doLast {
        val isWindows = System.getProperty("os.name").lowercase().contains("win")

        // Windows' ProcessBuilder does not resolve bare command names via PATH/PATHEXT
        // the way a shell does, so route through cmd /c there.
        fun run(vararg command: String): Boolean =
            try {
                val effectiveCommand = if (isWindows) listOf("cmd", "/c") + command else command.toList()
                val process = ProcessBuilder(effectiveCommand)
                    .directory(rootDir)
                    .redirectErrorStream(true)
                    .start()
                process.inputStream.bufferedReader().forEachLine(::println)
                process.waitFor() == 0
            } catch (e: java.io.IOException) {
                false
            }

        if (!isWindows) {
            run("chmod", "+x", "gradlew")
        }

        // No-op if core.hooksPath was never set; clears it if a clone still has it
        // pointed at the now-deleted .githooks/, which would otherwise silently
        // stop the hooks installed below from ever running.
        run("git", "config", "--unset", "core.hooksPath")

        if (!run("pre-commit", "--version")) {
            println("bootstrap: pre-commit not found, installing via pip")
            val installed = run("python3", "-m", "pip", "install", "--user", "pre-commit") ||
                run("python", "-m", "pip", "install", "--user", "pre-commit") ||
                run("py", "-m", "pip", "install", "--user", "pre-commit")
            check(installed) {
                "Could not install pre-commit. Install Python 3 and pip, then re-run this task."
            }
        }

        check(run("pre-commit", "install", "--hook-type", "pre-commit", "--hook-type", "pre-push")) {
            "pre-commit install failed. If pre-commit was just installed via pip --user, " +
                "make sure its install location is on your PATH, then re-run this task."
        }

        println("bootstrap: done. Pre-commit and pre-push hooks are wired.")
    }
}
