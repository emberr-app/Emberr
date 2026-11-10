package com.emberr.presentation.shared.editor.blockViews.property

import androidx.compose.runtime.Composable
import com.emberr.domain.model.PropertyBlock
import com.emberr.domain.model.PropertyType
import com.emberr.domain.model.PropertyValueType
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.property_type_checkbox
import emberr.shared.generated.resources.property_type_date
import emberr.shared.generated.resources.property_type_description
import emberr.shared.generated.resources.property_type_due_date
import emberr.shared.generated.resources.property_type_email
import emberr.shared.generated.resources.property_type_formula
import emberr.shared.generated.resources.property_type_link
import emberr.shared.generated.resources.property_type_name
import emberr.shared.generated.resources.property_type_number
import emberr.shared.generated.resources.property_type_people
import emberr.shared.generated.resources.property_type_phone
import emberr.shared.generated.resources.property_type_status
import emberr.shared.generated.resources.property_type_tags
import emberr.shared.generated.resources.property_value_type_checkbox
import emberr.shared.generated.resources.property_value_type_date
import emberr.shared.generated.resources.property_value_type_email
import emberr.shared.generated.resources.property_value_type_formula
import emberr.shared.generated.resources.property_value_type_link
import emberr.shared.generated.resources.property_value_type_number
import emberr.shared.generated.resources.property_value_type_person
import emberr.shared.generated.resources.property_value_type_phone
import emberr.shared.generated.resources.property_value_type_single_choice
import emberr.shared.generated.resources.property_value_type_tags
import emberr.shared.generated.resources.property_value_type_text
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

val PropertyType.labelResource: StringResource
    get() = when (this) {
        PropertyType.NAME -> Res.string.property_type_name
        PropertyType.PHONE -> Res.string.property_type_phone
        PropertyType.EMAIL -> Res.string.property_type_email
        PropertyType.DATE -> Res.string.property_type_date
        PropertyType.STATUS -> Res.string.property_type_status
        PropertyType.TAGS -> Res.string.property_type_tags
        PropertyType.PEOPLE -> Res.string.property_type_people
        PropertyType.LINK -> Res.string.property_type_link
        PropertyType.DESCRIPTION -> Res.string.property_type_description
        PropertyType.DUE_DATE -> Res.string.property_type_due_date
        PropertyType.CHECKBOX -> Res.string.property_type_checkbox
        PropertyType.NUMBER -> Res.string.property_type_number
        PropertyType.FORMULA -> Res.string.property_type_formula
    }

@Composable
fun PropertyType.displayName(): String = stringResource(labelResource)

@Composable
fun PropertyValueType.displayName(): String = stringResource(
    when (this) {
        PropertyValueType.TEXT -> Res.string.property_value_type_text
        PropertyValueType.PHONE -> Res.string.property_value_type_phone
        PropertyValueType.EMAIL -> Res.string.property_value_type_email
        PropertyValueType.LINK -> Res.string.property_value_type_link
        PropertyValueType.DATE -> Res.string.property_value_type_date
        PropertyValueType.SINGLE_CHOICE -> Res.string.property_value_type_single_choice
        PropertyValueType.TAGS -> Res.string.property_value_type_tags
        PropertyValueType.PERSON -> Res.string.property_value_type_person
        PropertyValueType.CHECKBOX -> Res.string.property_value_type_checkbox
        PropertyValueType.NUMBER -> Res.string.property_value_type_number
        PropertyValueType.FORMULA -> Res.string.property_value_type_formula
    }
)

@Composable
fun PropertyBlock.displayLabel(): String = propertyType?.displayName() ?: customLabel
