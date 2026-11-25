package com.kotdex.utils

import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals

class TimestampTest {
    private val timestamp = Timestamp(Date(1_700_000_000_123))

    @Test
    fun `formats in epoch seconds`() {
        assertEquals("<t:1700000000:t>", timestamp.time24())
        assertEquals("<t:1700000000:F>", timestamp.dateAndTimeFull())
    }
}
