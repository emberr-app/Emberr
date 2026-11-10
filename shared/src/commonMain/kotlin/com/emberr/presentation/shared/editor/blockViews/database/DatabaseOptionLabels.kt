package com.emberr.presentation.shared.editor.blockViews.database

import androidx.compose.runtime.Composable
import com.emberr.domain.model.DatabaseBlock
import com.emberr.domain.model.DatabaseCalculation
import com.emberr.domain.model.DatabaseCalculationGroup
import com.emberr.domain.model.DatabaseCardSize
import com.emberr.domain.model.DatabaseColorRuleTarget
import com.emberr.domain.model.DatabaseColumnTarget
import com.emberr.domain.model.DatabaseDateGrouping
import com.emberr.domain.model.DatabaseDateRange
import com.emberr.domain.model.DatabaseFilterCondition
import com.emberr.domain.model.DatabaseNumberDisplay
import com.emberr.domain.model.DatabaseNumberStyle
import com.emberr.domain.model.DatabaseRelativeDate
import com.emberr.domain.model.DatabaseRepeatFrequency
import com.emberr.domain.model.DatabaseViewType
import com.emberr.domain.model.labelOf
import com.emberr.presentation.shared.editor.blockViews.property.displayName
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.database_calculation_average
import emberr.shared.generated.resources.database_calculation_checked
import emberr.shared.generated.resources.database_calculation_count_all
import emberr.shared.generated.resources.database_calculation_count_empty
import emberr.shared.generated.resources.database_calculation_count_not_empty
import emberr.shared.generated.resources.database_calculation_count_unique_values
import emberr.shared.generated.resources.database_calculation_count_values
import emberr.shared.generated.resources.database_calculation_group_count
import emberr.shared.generated.resources.database_calculation_group_more
import emberr.shared.generated.resources.database_calculation_group_percent
import emberr.shared.generated.resources.database_calculation_max
import emberr.shared.generated.resources.database_calculation_median
import emberr.shared.generated.resources.database_calculation_min
import emberr.shared.generated.resources.database_calculation_percent_checked
import emberr.shared.generated.resources.database_calculation_percent_empty
import emberr.shared.generated.resources.database_calculation_percent_not_empty
import emberr.shared.generated.resources.database_calculation_percent_unchecked
import emberr.shared.generated.resources.database_calculation_range
import emberr.shared.generated.resources.database_calculation_short_count
import emberr.shared.generated.resources.database_calculation_short_empty
import emberr.shared.generated.resources.database_calculation_short_not_empty
import emberr.shared.generated.resources.database_calculation_short_unique
import emberr.shared.generated.resources.database_calculation_short_values
import emberr.shared.generated.resources.database_calculation_sum
import emberr.shared.generated.resources.database_calculation_unchecked
import emberr.shared.generated.resources.database_card_size_large
import emberr.shared.generated.resources.database_card_size_medium
import emberr.shared.generated.resources.database_card_size_small
import emberr.shared.generated.resources.database_color_rule_target_cell
import emberr.shared.generated.resources.database_color_rule_target_row
import emberr.shared.generated.resources.database_column_notes
import emberr.shared.generated.resources.database_condition_contains
import emberr.shared.generated.resources.database_condition_does_not_contain
import emberr.shared.generated.resources.database_condition_is
import emberr.shared.generated.resources.database_condition_is_after
import emberr.shared.generated.resources.database_condition_is_before
import emberr.shared.generated.resources.database_condition_is_checked
import emberr.shared.generated.resources.database_condition_is_empty
import emberr.shared.generated.resources.database_condition_is_greater_than
import emberr.shared.generated.resources.database_condition_is_less_than
import emberr.shared.generated.resources.database_condition_is_not
import emberr.shared.generated.resources.database_condition_is_not_empty
import emberr.shared.generated.resources.database_condition_is_on_or_after
import emberr.shared.generated.resources.database_condition_is_on_or_before
import emberr.shared.generated.resources.database_condition_is_unchecked
import emberr.shared.generated.resources.database_condition_is_within
import emberr.shared.generated.resources.database_date_grouping_day
import emberr.shared.generated.resources.database_date_grouping_month
import emberr.shared.generated.resources.database_date_grouping_week
import emberr.shared.generated.resources.database_date_grouping_year
import emberr.shared.generated.resources.database_date_range_next_30_days
import emberr.shared.generated.resources.database_date_range_next_7_days
import emberr.shared.generated.resources.database_date_range_past_30_days
import emberr.shared.generated.resources.database_date_range_past_7_days
import emberr.shared.generated.resources.database_date_range_this_month
import emberr.shared.generated.resources.database_date_range_this_week
import emberr.shared.generated.resources.database_date_range_this_year
import emberr.shared.generated.resources.database_number_display_bar
import emberr.shared.generated.resources.database_number_display_number
import emberr.shared.generated.resources.database_number_display_ring
import emberr.shared.generated.resources.database_number_style_euro
import emberr.shared.generated.resources.database_number_style_number
import emberr.shared.generated.resources.database_number_style_number_with_commas
import emberr.shared.generated.resources.database_number_style_percent
import emberr.shared.generated.resources.database_number_style_pound
import emberr.shared.generated.resources.database_number_style_rupee
import emberr.shared.generated.resources.database_number_style_us_dollar
import emberr.shared.generated.resources.database_number_style_yen
import emberr.shared.generated.resources.database_relative_date_one_month_ago
import emberr.shared.generated.resources.database_relative_date_one_month_from_now
import emberr.shared.generated.resources.database_relative_date_one_week_ago
import emberr.shared.generated.resources.database_relative_date_one_week_from_now
import emberr.shared.generated.resources.database_relative_date_today
import emberr.shared.generated.resources.database_relative_date_tomorrow
import emberr.shared.generated.resources.database_relative_date_yesterday
import emberr.shared.generated.resources.database_repeat_frequency_daily
import emberr.shared.generated.resources.database_repeat_frequency_monthly
import emberr.shared.generated.resources.database_repeat_frequency_weekdays
import emberr.shared.generated.resources.database_repeat_frequency_weekly
import emberr.shared.generated.resources.database_view_type_board
import emberr.shared.generated.resources.database_view_type_gallery
import emberr.shared.generated.resources.database_view_type_table
import org.jetbrains.compose.resources.stringResource

@Composable
fun DatabaseFilterCondition.displayName(): String = stringResource(
    when (this) {
        DatabaseFilterCondition.CONTAINS -> Res.string.database_condition_contains
        DatabaseFilterCondition.DOES_NOT_CONTAIN -> Res.string.database_condition_does_not_contain
        DatabaseFilterCondition.IS -> Res.string.database_condition_is
        DatabaseFilterCondition.IS_NOT -> Res.string.database_condition_is_not
        DatabaseFilterCondition.IS_BEFORE -> Res.string.database_condition_is_before
        DatabaseFilterCondition.IS_AFTER -> Res.string.database_condition_is_after
        DatabaseFilterCondition.IS_ON_OR_BEFORE -> Res.string.database_condition_is_on_or_before
        DatabaseFilterCondition.IS_ON_OR_AFTER -> Res.string.database_condition_is_on_or_after
        DatabaseFilterCondition.IS_WITHIN -> Res.string.database_condition_is_within
        DatabaseFilterCondition.IS_EMPTY -> Res.string.database_condition_is_empty
        DatabaseFilterCondition.IS_NOT_EMPTY -> Res.string.database_condition_is_not_empty
        DatabaseFilterCondition.IS_CHECKED -> Res.string.database_condition_is_checked
        DatabaseFilterCondition.IS_UNCHECKED -> Res.string.database_condition_is_unchecked
        DatabaseFilterCondition.IS_GREATER_THAN -> Res.string.database_condition_is_greater_than
        DatabaseFilterCondition.IS_LESS_THAN -> Res.string.database_condition_is_less_than
    }
)

@Composable
fun DatabaseRelativeDate.displayName(): String = stringResource(
    when (this) {
        DatabaseRelativeDate.TODAY -> Res.string.database_relative_date_today
        DatabaseRelativeDate.TOMORROW -> Res.string.database_relative_date_tomorrow
        DatabaseRelativeDate.YESTERDAY -> Res.string.database_relative_date_yesterday
        DatabaseRelativeDate.ONE_WEEK_AGO -> Res.string.database_relative_date_one_week_ago
        DatabaseRelativeDate.ONE_WEEK_FROM_NOW -> Res.string.database_relative_date_one_week_from_now
        DatabaseRelativeDate.ONE_MONTH_AGO -> Res.string.database_relative_date_one_month_ago
        DatabaseRelativeDate.ONE_MONTH_FROM_NOW -> Res.string.database_relative_date_one_month_from_now
    }
)

@Composable
fun DatabaseDateRange.displayName(): String = stringResource(
    when (this) {
        DatabaseDateRange.THIS_WEEK -> Res.string.database_date_range_this_week
        DatabaseDateRange.THIS_MONTH -> Res.string.database_date_range_this_month
        DatabaseDateRange.THIS_YEAR -> Res.string.database_date_range_this_year
        DatabaseDateRange.PAST_7_DAYS -> Res.string.database_date_range_past_7_days
        DatabaseDateRange.NEXT_7_DAYS -> Res.string.database_date_range_next_7_days
        DatabaseDateRange.PAST_30_DAYS -> Res.string.database_date_range_past_30_days
        DatabaseDateRange.NEXT_30_DAYS -> Res.string.database_date_range_next_30_days
    }
)

@Composable
fun DatabaseViewType.displayName(): String = stringResource(
    when (this) {
        DatabaseViewType.TABLE -> Res.string.database_view_type_table
        DatabaseViewType.GALLERY -> Res.string.database_view_type_gallery
        DatabaseViewType.BOARD -> Res.string.database_view_type_board
    }
)

@Composable
fun DatabaseDateGrouping.displayName(): String = stringResource(
    when (this) {
        DatabaseDateGrouping.DAY -> Res.string.database_date_grouping_day
        DatabaseDateGrouping.WEEK -> Res.string.database_date_grouping_week
        DatabaseDateGrouping.MONTH -> Res.string.database_date_grouping_month
        DatabaseDateGrouping.YEAR -> Res.string.database_date_grouping_year
    }
)

@Composable
fun DatabaseCardSize.displayName(): String = stringResource(
    when (this) {
        DatabaseCardSize.SMALL -> Res.string.database_card_size_small
        DatabaseCardSize.MEDIUM -> Res.string.database_card_size_medium
        DatabaseCardSize.LARGE -> Res.string.database_card_size_large
    }
)

@Composable
fun DatabaseCalculationGroup.displayName(): String = stringResource(
    when (this) {
        DatabaseCalculationGroup.COUNT -> Res.string.database_calculation_group_count
        DatabaseCalculationGroup.PERCENT -> Res.string.database_calculation_group_percent
        DatabaseCalculationGroup.MORE -> Res.string.database_calculation_group_more
    }
)

@Composable
fun DatabaseCalculation.displayName(): String = stringResource(
    when (this) {
        DatabaseCalculation.COUNT_ALL -> Res.string.database_calculation_count_all
        DatabaseCalculation.COUNT_VALUES -> Res.string.database_calculation_count_values
        DatabaseCalculation.COUNT_UNIQUE_VALUES -> Res.string.database_calculation_count_unique_values
        DatabaseCalculation.COUNT_EMPTY -> Res.string.database_calculation_count_empty
        DatabaseCalculation.COUNT_NOT_EMPTY -> Res.string.database_calculation_count_not_empty
        DatabaseCalculation.CHECKED -> Res.string.database_calculation_checked
        DatabaseCalculation.UNCHECKED -> Res.string.database_calculation_unchecked
        DatabaseCalculation.PERCENT_EMPTY -> Res.string.database_calculation_percent_empty
        DatabaseCalculation.PERCENT_NOT_EMPTY -> Res.string.database_calculation_percent_not_empty
        DatabaseCalculation.PERCENT_CHECKED -> Res.string.database_calculation_percent_checked
        DatabaseCalculation.PERCENT_UNCHECKED -> Res.string.database_calculation_percent_unchecked
        DatabaseCalculation.SUM -> Res.string.database_calculation_sum
        DatabaseCalculation.AVERAGE -> Res.string.database_calculation_average
        DatabaseCalculation.MEDIAN -> Res.string.database_calculation_median
        DatabaseCalculation.MIN -> Res.string.database_calculation_min
        DatabaseCalculation.MAX -> Res.string.database_calculation_max
        DatabaseCalculation.RANGE -> Res.string.database_calculation_range
    }
)

@Composable
fun DatabaseColorRuleTarget.displayName(): String = stringResource(
    when (this) {
        DatabaseColorRuleTarget.ROW -> Res.string.database_color_rule_target_row
        DatabaseColorRuleTarget.CELL -> Res.string.database_color_rule_target_cell
    }
)

@Composable
fun DatabaseRepeatFrequency.displayName(): String = stringResource(
    when (this) {
        DatabaseRepeatFrequency.DAILY -> Res.string.database_repeat_frequency_daily
        DatabaseRepeatFrequency.WEEKDAYS -> Res.string.database_repeat_frequency_weekdays
        DatabaseRepeatFrequency.WEEKLY -> Res.string.database_repeat_frequency_weekly
        DatabaseRepeatFrequency.MONTHLY -> Res.string.database_repeat_frequency_monthly
    }
)

@Composable
fun DatabaseNumberStyle.displayName(): String = stringResource(
    when (this) {
        DatabaseNumberStyle.NUMBER -> Res.string.database_number_style_number
        DatabaseNumberStyle.NUMBER_WITH_COMMAS -> Res.string.database_number_style_number_with_commas
        DatabaseNumberStyle.PERCENT -> Res.string.database_number_style_percent
        DatabaseNumberStyle.US_DOLLAR -> Res.string.database_number_style_us_dollar
        DatabaseNumberStyle.EURO -> Res.string.database_number_style_euro
        DatabaseNumberStyle.POUND -> Res.string.database_number_style_pound
        DatabaseNumberStyle.RUPEE -> Res.string.database_number_style_rupee
        DatabaseNumberStyle.YEN -> Res.string.database_number_style_yen
    }
)

@Composable
fun DatabaseNumberDisplay.displayName(): String = stringResource(
    when (this) {
        DatabaseNumberDisplay.NUMBER -> Res.string.database_number_display_number
        DatabaseNumberDisplay.BAR -> Res.string.database_number_display_bar
        DatabaseNumberDisplay.RING -> Res.string.database_number_display_ring
    }
)

@Composable
fun DatabaseCalculation.shortDisplayName(): String = stringResource(
    when (this) {
        DatabaseCalculation.COUNT_ALL -> Res.string.database_calculation_short_count
        DatabaseCalculation.COUNT_VALUES -> Res.string.database_calculation_short_values
        DatabaseCalculation.COUNT_UNIQUE_VALUES -> Res.string.database_calculation_short_unique
        DatabaseCalculation.COUNT_EMPTY -> Res.string.database_calculation_short_empty
        DatabaseCalculation.COUNT_NOT_EMPTY -> Res.string.database_calculation_short_not_empty
        DatabaseCalculation.CHECKED -> Res.string.database_calculation_checked
        DatabaseCalculation.UNCHECKED -> Res.string.database_calculation_unchecked
        DatabaseCalculation.PERCENT_EMPTY -> Res.string.database_calculation_short_empty
        DatabaseCalculation.PERCENT_NOT_EMPTY -> Res.string.database_calculation_short_not_empty
        DatabaseCalculation.PERCENT_CHECKED -> Res.string.database_calculation_checked
        DatabaseCalculation.PERCENT_UNCHECKED -> Res.string.database_calculation_unchecked
        DatabaseCalculation.SUM -> Res.string.database_calculation_sum
        DatabaseCalculation.AVERAGE -> Res.string.database_calculation_average
        DatabaseCalculation.MEDIAN -> Res.string.database_calculation_median
        DatabaseCalculation.MIN -> Res.string.database_calculation_min
        DatabaseCalculation.MAX -> Res.string.database_calculation_max
        DatabaseCalculation.RANGE -> Res.string.database_calculation_range
    }
)

@Composable
fun DatabaseBlock.displayLabelOf(column: DatabaseColumnTarget): String = when (column) {
    DatabaseColumnTarget.NotesTitle -> stringResource(Res.string.database_column_notes)
    is DatabaseColumnTarget.Property -> column.propertyType.displayName()
    is DatabaseColumnTarget.CustomProperty -> labelOf(column)
}
