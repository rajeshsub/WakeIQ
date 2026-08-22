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
