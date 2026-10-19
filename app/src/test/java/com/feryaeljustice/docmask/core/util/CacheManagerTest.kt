package com.feryaeljustice.docmask.core.util

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Unit tests verifying CacheManager directory scanning, size calculation, and recursive cleanup.
 */
class CacheManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val context: Context = mockk()

    @Test
    fun givenEmptyDirectory_whenGetStatsCalled_thenReturnsZeroFilesAndBytes() {
        // Given
        val cacheDir = tempFolder.newFolder("cache")
        every { context.cacheDir } returns cacheDir

        // When
        val (count, bytes) = CacheManager.getTemporaryStorageStats(context)

        // Then
        assertEquals(0, count)
        assertEquals(0L, bytes)
    }

    @Test
    fun givenPopulatedDirectory_whenGetStatsCalled_thenReturnsAccurateCountAndBytes() {
        // Given
        val cacheDir = tempFolder.newFolder("cache")
        val exportDir = File(cacheDir, "exported_documents").apply { mkdirs() }
        val file1 = File(exportDir, "doc1.png").apply { writeText("Hello World!") }
        val file2 = File(exportDir, "doc2.png").apply { writeText("DocMask Security") }
        every { context.cacheDir } returns cacheDir

        // When
        val (count, bytes) = CacheManager.getTemporaryStorageStats(context)

        // Then
        assertEquals(2, count)
        assertEquals(file1.length() + file2.length(), bytes)
    }

    @Test
    fun givenTemporaryFiles_whenClearTemporaryStorageCalled_thenDeletesFilesAndReturnsFreedMetrics() {
        // Given
        val cacheDir = tempFolder.newFolder("cache")
        val exportDir = File(cacheDir, "exported_documents").apply { mkdirs() }
        val file1 = File(exportDir, "doc1.png").apply { writeText("Confidential ID") }
        val expectedLength = file1.length()
        every { context.cacheDir } returns cacheDir

        // When
        val (deletedCount, freedBytes) = CacheManager.clearTemporaryStorage(context)

        // Then
        assertEquals(1, deletedCount)
        assertEquals(expectedLength, freedBytes)
        val (newCount, newBytes) = CacheManager.getTemporaryStorageStats(context)
        assertEquals(0, newCount)
        assertEquals(0L, newBytes)
    }

    @Test
    fun givenByteSizes_whenFormatBytesCalled_thenFormatsUnitsCorrectly() {
        assertEquals("0 B", CacheManager.formatBytes(0))
        assertEquals("500 B", CacheManager.formatBytes(500))
        assertEquals("1.0 KB", CacheManager.formatBytes(1024))
        assertEquals("2.5 MB", CacheManager.formatBytes(2621440))
    }
}
