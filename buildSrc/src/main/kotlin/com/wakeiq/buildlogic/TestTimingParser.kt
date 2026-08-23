package com.wakeiq.buildlogic

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

data class TestCaseTiming(val className: String, val name: String, val seconds: Double)

data class TestTimingResult(
    val totalSeconds: Double,
    val testCount: Int,
    // Slowest first - a chained/scenario test (multiple units of production
    // code exercised in sequence) surfaces here the same as a single-unit
    // test; JUnit XML doesn't distinguish them, and neither does this.
    val testCases: List<TestCaseTiming>,
)

// Aggregates Gradle's own JUnit XML test reports; see docs/adr/0007.
object TestTimingParser {
    fun parse(xmlFiles: List<File>): TestTimingResult {
        var totalSeconds = 0.0
        var testCount = 0
        val testCases = mutableListOf<TestCaseTiming>()
        val factory = DocumentBuilderFactory.newInstance()

        for (file in xmlFiles) {
            val root = factory.newDocumentBuilder().parse(file).documentElement
            totalSeconds += root.getAttribute("time").toDoubleOrNull() ?: 0.0
            testCount += root.getAttribute("tests").toIntOrNull() ?: 0

            val testcaseNodes = root.getElementsByTagName("testcase")
            for (i in 0 until testcaseNodes.length) {
                val node = testcaseNodes.item(i)
                val className = node.attributes.getNamedItem("classname")?.nodeValue ?: ""
                val name = node.attributes.getNamedItem("name")?.nodeValue ?: ""
                val seconds = node.attributes.getNamedItem("time")?.nodeValue?.toDoubleOrNull() ?: 0.0
                testCases += TestCaseTiming(className, name, seconds)
            }
        }

        return TestTimingResult(totalSeconds, testCount, testCases.sortedByDescending { it.seconds })
    }
}
