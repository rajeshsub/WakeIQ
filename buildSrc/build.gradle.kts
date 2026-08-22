plugins {
    `kotlin-dsl`
    // buildSrc is a standalone build with no access to the root project's
    // version catalog, so these versions are repeated here - keep them in
    // sync with gradle/libs.versions.toml's ktlintPlugin/detekt entries.
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
    id("io.gitlab.arturbosch.detekt") version "1.23.7"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // detekt.yml's `formatting:` block needs this module on detekt's own
    // classpath, same as app/build.gradle.kts's detektPlugins dependency.
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.7")
}

tasks.test {
    useJUnitPlatform()
}

detekt {
    // buildSrc is its own standalone build, so "rootProject" here is buildSrc
    // itself, not WakeIQ's root - the shared config lives one directory up.
    config.setFrom(files("../detekt.yml"))
    buildUponDefaultConfig = true
    allRules = false
}
