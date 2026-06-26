package com.wakeiq.core

/**
 * Marks a class or function as a thin adapter over an Android framework API whose only meaningful
 * behaviour is the framework call itself. Such code cannot be exercised by JVM unit tests without
 * mocking the framework, so its pure logic is extracted into separate tested units and the adapter
 * is excluded from the Kover logic coverage report.
 *
 * Excluding code from coverage does not lower its quality bar: ktlint, detekt and Android Lint still
 * apply, and instrumented tests exercise the real flows. See docs/adr/0002-coverage-exclusions.md.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class InstrumentedOnly
