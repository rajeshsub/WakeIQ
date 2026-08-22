package com.wakeiq.buildlogic

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

data class TestTimingResult(val totalSeconds: Double, val testCount: Int)

// Aggregates Gradle's own JUnit XML test reports; see docs/adr/0007.
object TestTimingParser {
    fun parse(xmlFiles: List<File>): TestTimingResult {
        var totalSeconds = 0.0
        var testCount = 0
        val factory = DocumentBuilderFactory.newInstance()

        for (file in xmlFiles) {
            val root = factory.newDocumentBuilder().parse(file).documentElement
            totalSeconds += root.getAttribute("time").toDoubleOrNull() ?: 0.0
            testCount += root.getAttribute("tests").toIntOrNull() ?: 0
        }

        return TestTimingResult(totalSeconds, testCount)
    }
}
