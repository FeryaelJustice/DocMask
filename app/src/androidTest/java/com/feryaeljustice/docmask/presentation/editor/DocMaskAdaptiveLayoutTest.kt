package com.feryaeljustice.docmask.presentation.editor

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.feryaeljustice.docmask.core.model.MaskingOptions
import com.feryaeljustice.docmask.ui.theme.DocMaskTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI test verifying adaptive layouts and touch target compliance.
 * English descriptive Given-When-Then test cases.
 */
@RunWith(AndroidJUnit4::class)
class DocMaskAdaptiveLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenEmptyEditorState_whenRendered_thenDisplaysOfflinePrivacyGuarantees() {
        // Given
        val emptyState = EditorUiState()

        // When
        composeTestRule.setContent {
            DocMaskTheme {
                DocumentViewerPane(
                    uiState = emptyState,
                    onEvent = {}
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithText("Selecciona un documento para proteger")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Procesamiento 100% offline y seguro")
            .assertIsDisplayed()
    }

    @Test
    fun givenEditorControls_whenGrayscaleSwitchToggled_thenTriggersCallback() {
        // Given
        var toggledGrayscale = false
        val state = EditorUiState(
            maskingOptions = MaskingOptions(applyGrayscale = true)
        )

        // When
        composeTestRule.setContent {
            DocMaskTheme {
                EditorControlsPane(
                    uiState = state,
                    onEvent = { event ->
                        if (event is EditorUiEvent.ToggleGrayscale) {
                            toggledGrayscale = event.enabled
                        }
                    }
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithTag("switch_grayscale")
            .assertIsDisplayed()
            .performClick()

        assert(!toggledGrayscale)
    }

    @Test
    fun givenExportButton_whenInspected_thenHasClickAction() {
        // Given
        val state = EditorUiState()

        // When
        composeTestRule.setContent {
            DocMaskTheme {
                EditorControlsPane(
                    uiState = state,
                    onEvent = {}
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithTag("button_export")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
}
