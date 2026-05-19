package com.lifetrio.memo

import org.junit.Assert.assertTrue
import org.junit.Test

class MemoSearchTest {
    @Test
    fun searchCanMatchTitleBodyOrTags() {
        val title = "项目灵感"
        val body = "把备忘转成计�?
        val tags = listOf("工作", "灵感")

        fun matches(query: String): Boolean =
            title.contains(query) || body.contains(query) || tags.any { it.contains(query) }

        assertTrue(matches("项目"))
        assertTrue(matches("计划"))
        assertTrue(matches("工作"))
    }
}
