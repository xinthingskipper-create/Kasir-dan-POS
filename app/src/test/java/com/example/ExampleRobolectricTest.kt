package com.example

import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleRobolectricTest {

  @Test
  fun `verify OrderType labels`() {
    assertEquals("Makan di Tempat", com.example.data.model.OrderType.DINE_IN.label)
    assertEquals("Di Bungkus", com.example.data.model.OrderType.TAKE_AWAY.label)
  }

  @Test
  fun `verify CloudCategoryTab labels`() {
    assertEquals("Source Code & Panduan Web", com.example.ui.screens.CloudCategoryTab.SOURCE_CODE.label)
    assertEquals("Semua", com.example.ui.screens.CloudCategoryTab.SEMUA.label)
  }

  @Test
  fun `verify SourceCodeSubTab enums`() {
    assertEquals("database.rules.json", com.example.ui.screens.SourceCodeSubTab.RTDB_RULES.fileName)
    assertEquals("realtime_database_schema.json", com.example.ui.screens.SourceCodeSubTab.REPORT_TABLES.fileName)
  }
}
