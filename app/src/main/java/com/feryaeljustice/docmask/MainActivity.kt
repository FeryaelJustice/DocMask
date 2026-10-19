package com.feryaeljustice.docmask

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.feryaeljustice.docmask.core.util.BiometricHelper
import com.feryaeljustice.docmask.data.local.preferences.UserPreferencesRepository
import com.feryaeljustice.docmask.presentation.editor.DocMaskApp
import com.feryaeljustice.docmask.presentation.security.LockScreen
import com.feryaeljustice.docmask.ui.theme.DocMaskTheme
import org.koin.android.ext.android.inject

class MainActivity : FragmentActivity() {

    private val userPreferencesRepository: UserPreferencesRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isBiometricEnabled by userPreferencesRepository.isBiometricEnabled.collectAsState(initial = null)
            var isAuthenticated by remember { mutableStateOf(false) }

            LaunchedEffect(isBiometricEnabled) {
                if (isBiometricEnabled == true && !isAuthenticated) {
                    BiometricHelper.showBiometricPrompt(
                        activity = this@MainActivity,
                        onSuccess = { isAuthenticated = true },
                        onError = { /* Keep locked until user taps unlock button */ }
                    )
                }
            }

            DocMaskTheme {
                when {
                    isBiometricEnabled == null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    isBiometricEnabled == true && !isAuthenticated -> {
                        LockScreen(
                            onUnlockClick = {
                                BiometricHelper.showBiometricPrompt(
                                    activity = this@MainActivity,
                                    onSuccess = { isAuthenticated = true },
                                    onError = { /* Stays locked */ }
                                )
                            }
                        )
                    }
                    else -> {
                        DocMaskApp()
                    }
                }
            }
        }
    }
}