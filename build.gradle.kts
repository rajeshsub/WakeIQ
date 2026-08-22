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
    alias(libs.plugins.owasp.dependencycheck)
}

// Scans the whole resolved dependency graph (all modules), so it's applied at
// the root rather than per-module. See docs/adr/0006-dependency-locking-and-vulnerability-scanning.md.
dependencyCheck {
    failBuildOnCVSS = 7.0f
    suppressionFile = "owasp-suppressions.xml"
    formats = listOf("HTML", "JUNIT")
    // Set explicitly (docs default is ${buildDir}/reports) so the report path
    // CI uploads as an artifact is fixed and documented, not left to a default
    // that could shift between plugin versions.
    outputDirectory.set(layout.buildDirectory.dir("reports/dependency-check"))
    // Set explicitly (outside build/, since `clean` would otherwise wipe the
    // synced CVE database) so CI can cache this exact path across runs instead
    // of re-syncing the full NVD feed every time. See ci.yml / dependency-scan.yml.
    data {
        directory = "${rootDir}/.dependency-check-data"
    }
    nvd {
        // Optional but strongly recommended: an unset key works, just far slower
        // (NVD rate-limits anonymous API access). Set the NVD_API_KEY secret in CI
        // and export it locally to speed up the first scan after cache expiry.
        apiKey = System.getenv("NVD_API_KEY")
    }
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
