package com.emberr.presentation.people

import androidx.compose.runtime.Composable
import com.emberr.domain.people.ContactToImport

@Composable
expect fun rememberPhoneContactPicker(onContactsPicked: (List<ContactToImport>) -> Unit): (() -> Unit)?
