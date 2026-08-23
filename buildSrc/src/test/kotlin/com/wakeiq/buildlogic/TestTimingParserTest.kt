package com.wakeiq.buildlogic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class TestTimingParserTest {

    @TempDir
    lateinit var tempDir: File

    private fun writeXml(name: String, content: String): File = File(tempDir, name).apply { writeText(content) }

    @Test
    fun `single file with valid attributes sums correctly`() {
        val file = writeXml("a.xml", """<testsuite time="1.5" tests="3"></testsuite>""")

        val result = TestTimingParser.parse(listOf(file))

        assertEquals(1.5, result.totalSeconds)
        assertEquals(3, result.testCount)
    }

    @Test
    fun `multiple files sum across all of them`() {
        val fileA = writeXml("a.xml", """<testsuite time="1.5" tests="3"></testsuite>""")
        val fileB = writeXml("b.xml", """<testsuite time="2.25" tests="5"></testsuite>""")

        val result = TestTimingParser.parse(listOf(fileA, fileB))

        assertEquals(3.75, result.totalSeconds)
        assertEquals(8, result.testCount)
    }

    @Test
    fun `per-testcase durations are extracted and sorted slowest first`() {
        val file = writeXml(
            "a.xml",
            """
            <testsuite time="0.010" tests="2">
              <testcase name="fast case" classname="com.wakeiq.FooTest" time="0.002"/>
              <testcase name="slow case" classname="com.wakeiq.FooTest" time="0.008"/>
            </testsuite>
            """.trimIndent(),
        )

        val result = TestTimingParser.parse(listOf(file))

        assertEquals(
            listOf(
                TestCaseTiming("com.wakeiq.FooTest", "slow case", 0.008),
                TestCaseTiming("com.wakeiq.FooTest", "fast case", 0.002),
            ),
            result.testCases,
        )
    }

    @Test
    fun `testcase missing time attribute defaults to zero`() {
        val file = writeXml(
            "a.xml",
            """
            <testsuite time="0.0" tests="1">
              <testcase name="no time" classname="com.wakeiq.FooTest"/>
            </testsuite>
            """.trimIndent(),
        )

        val result = TestTimingParser.parse(listOf(file))

        assertEquals(listOf(TestCaseTiming("com.wakeiq.FooTest", "no time", 0.0)), result.testCases)
    }

    @Test
    fun `missing time attribute defaults to zero without throwing`() {
        val file = writeXml("a.xml", """<testsuite tests="3"></testsuite>""")

        val result = TestTimingParser.parse(listOf(file))

        assertEquals(0.0, result.totalSeconds)
        assertEquals(3, result.testCount)
    }

    @Test
    fun `malformed tests attribute defaults to zero without throwing`() {
        val file = writeXml("a.xml", """<testsuite time="1.5" tests="not-a-number"></testsuite>""")

        val result = TestTimingParser.parse(listOf(file))

        assertEquals(1.5, result.totalSeconds)
        assertEquals(0, result.testCount)
    }

    @Test
    fun `empty file list returns zero totals`() {
        val result = TestTimingParser.parse(emptyList())

        assertEquals(0.0, result.totalSeconds)
        assertEquals(0, result.testCount)
    }
}
