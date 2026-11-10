package com.emberr.presentation.people

import androidx.compose.runtime.Composable
import com.emberr.domain.model.PersonDetailKind
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.common_untitled
import emberr.shared.generated.resources.people_add_address
import emberr.shared.generated.resources.people_add_birthday
import emberr.shared.generated.resources.people_add_company
import emberr.shared.generated.resources.people_add_custom
import emberr.shared.generated.resources.people_add_email
import emberr.shared.generated.resources.people_add_phone
import emberr.shared.generated.resources.people_add_relationship
import emberr.shared.generated.resources.people_add_role
import emberr.shared.generated.resources.people_add_social
import emberr.shared.generated.resources.people_add_website
import emberr.shared.generated.resources.people_detail_address
import emberr.shared.generated.resources.people_detail_birthday
import emberr.shared.generated.resources.people_detail_company
import emberr.shared.generated.resources.people_detail_custom
import emberr.shared.generated.resources.people_detail_email
import emberr.shared.generated.resources.people_detail_phone
import emberr.shared.generated.resources.people_detail_relationship
import emberr.shared.generated.resources.people_detail_role
import emberr.shared.generated.resources.people_detail_social
import emberr.shared.generated.resources.people_detail_website
import emberr.shared.generated.resources.people_edit_address
import emberr.shared.generated.resources.people_edit_birthday
import emberr.shared.generated.resources.people_edit_company
import emberr.shared.generated.resources.people_edit_custom
import emberr.shared.generated.resources.people_edit_email
import emberr.shared.generated.resources.people_edit_phone
import emberr.shared.generated.resources.people_edit_relationship
import emberr.shared.generated.resources.people_edit_role
import emberr.shared.generated.resources.people_edit_social
import emberr.shared.generated.resources.people_edit_website
import emberr.shared.generated.resources.saved_name_label_anniversary
import emberr.shared.generated.resources.saved_name_label_birthday
import emberr.shared.generated.resources.saved_name_label_blog
import emberr.shared.generated.resources.saved_name_label_date
import emberr.shared.generated.resources.saved_name_label_home
import emberr.shared.generated.resources.saved_name_label_main
import emberr.shared.generated.resources.saved_name_label_mobile
import emberr.shared.generated.resources.saved_name_label_nickname
import emberr.shared.generated.resources.saved_name_label_personal
import emberr.shared.generated.resources.saved_name_label_portfolio
import emberr.shared.generated.resources.saved_name_label_work
import org.jetbrains.compose.resources.stringResource

@Composable
fun PersonDetailKind.displayName(): String = stringResource(
    when (this) {
        PersonDetailKind.PHONE -> Res.string.people_detail_phone
        PersonDetailKind.EMAIL -> Res.string.people_detail_email
        PersonDetailKind.BIRTHDAY -> Res.string.people_detail_birthday
        PersonDetailKind.ADDRESS -> Res.string.people_detail_address
        PersonDetailKind.COMPANY -> Res.string.people_detail_company
        PersonDetailKind.ROLE -> Res.string.people_detail_role
        PersonDetailKind.WEBSITE -> Res.string.people_detail_website
        PersonDetailKind.SOCIAL -> Res.string.people_detail_social
        PersonDetailKind.RELATIONSHIP -> Res.string.people_detail_relationship
        PersonDetailKind.CUSTOM -> Res.string.people_detail_custom
    }
)

@Composable
internal fun PersonDetailKind.addDetailTitle(): String = stringResource(
    when (this) {
        PersonDetailKind.PHONE -> Res.string.people_add_phone
        PersonDetailKind.EMAIL -> Res.string.people_add_email
        PersonDetailKind.BIRTHDAY -> Res.string.people_add_birthday
        PersonDetailKind.ADDRESS -> Res.string.people_add_address
        PersonDetailKind.COMPANY -> Res.string.people_add_company
        PersonDetailKind.ROLE -> Res.string.people_add_role
        PersonDetailKind.WEBSITE -> Res.string.people_add_website
        PersonDetailKind.SOCIAL -> Res.string.people_add_social
        PersonDetailKind.RELATIONSHIP -> Res.string.people_add_relationship
        PersonDetailKind.CUSTOM -> Res.string.people_add_custom
    }
)

@Composable
internal fun PersonDetailKind.editDetailTitle(): String = stringResource(
    when (this) {
        PersonDetailKind.PHONE -> Res.string.people_edit_phone
        PersonDetailKind.EMAIL -> Res.string.people_edit_email
        PersonDetailKind.BIRTHDAY -> Res.string.people_edit_birthday
        PersonDetailKind.ADDRESS -> Res.string.people_edit_address
        PersonDetailKind.COMPANY -> Res.string.people_edit_company
        PersonDetailKind.ROLE -> Res.string.people_edit_role
        PersonDetailKind.WEBSITE -> Res.string.people_edit_website
        PersonDetailKind.SOCIAL -> Res.string.people_edit_social
        PersonDetailKind.RELATIONSHIP -> Res.string.people_edit_relationship
        PersonDetailKind.CUSTOM -> Res.string.people_edit_custom
    }
)

@Composable
internal fun shownPersonName(title: String): String = title.trim().ifEmpty { stringResource(Res.string.common_untitled) }

private val detailLabelPhrases = mapOf(
    "Mobile" to Res.string.saved_name_label_mobile,
    "Home" to Res.string.saved_name_label_home,
    "Work" to Res.string.saved_name_label_work,
    "Main" to Res.string.saved_name_label_main,
    "Personal" to Res.string.saved_name_label_personal,
    "Portfolio" to Res.string.saved_name_label_portfolio,
    "Blog" to Res.string.saved_name_label_blog,
    "Anniversary" to Res.string.saved_name_label_anniversary,
    "Date" to Res.string.saved_name_label_date,
    "Nickname" to Res.string.saved_name_label_nickname,
    "Birthday" to Res.string.saved_name_label_birthday
)

@Composable
internal fun shownDetailLabel(storedLabel: String): String =
    detailLabelPhrases[storedLabel.trim()]?.let { stringResource(it) } ?: storedLabel
