package com.lifetrio.ledger

import com.lifetrio.core.data.db.entity.toAmountCents
import com.lifetrio.core.data.db.entity.toYuanText
import org.junit.Assert.assertEquals
import org.junit.Test

class LedgerMathTest {
    @Test
    fun convertsYuanInputToCents() {
        assertEquals(1234L, "12.34".toAmountCents())
        assertEquals(1200L, "12".toAmountCents())
    }

    @Test
    fun convertsCentsToReadableYuan() {
        assertEquals("12.34", 1234L.toYuanText())
        assertEquals("12", 1200L.toYuanText())
    }
}
