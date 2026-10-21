package com.feryaeljustice.docmask.core.model

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.feryaeljustice.docmask.R

/**
 * Represents the side of a two-sided document (e.g. Spanish DNI front and back).
 */
enum class DocumentSide(
    @StringRes val titleRes: Int,
    val displayName: String
) {
    FRONT(R.string.editor_side_front, "Anverso"),
    BACK(R.string.editor_side_back, "Reverso");

    fun getLabel(context: Context): String = context.getString(titleRes)

    @Composable
    fun getLabel(): String = stringResource(titleRes)
}
