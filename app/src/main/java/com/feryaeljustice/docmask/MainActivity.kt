package com.feryaeljustice.docmask

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.feryaeljustice.docmask.presentation.editor.DocMaskApp
import com.feryaeljustice.docmask.ui.theme.DocMaskTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DocMaskTheme {
                DocMaskApp()
            }
        }
    }
}