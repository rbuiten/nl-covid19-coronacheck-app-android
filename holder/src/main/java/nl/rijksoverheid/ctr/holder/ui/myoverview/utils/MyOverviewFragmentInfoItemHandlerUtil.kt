package nl.rijksoverheid.ctr.holder.ui.myoverview.utils

import android.content.Intent
import android.provider.Settings
import nl.rijksoverheid.ctr.design.ext.formatDayMonthTime
import nl.rijksoverheid.ctr.design.utils.BottomSheetData
import nl.rijksoverheid.ctr.design.utils.BottomSheetDialogUtil
import nl.rijksoverheid.ctr.design.utils.DescriptionData
import nl.rijksoverheid.ctr.holder.R
import nl.rijksoverheid.ctr.holder.persistence.database.entities.GreenCardType
import nl.rijksoverheid.ctr.holder.persistence.database.entities.OriginType
import nl.rijksoverheid.ctr.holder.ui.create_qr.models.DashboardItem
import nl.rijksoverheid.ctr.holder.ui.myoverview.MyOverviewFragment
import nl.rijksoverheid.ctr.holder.ui.myoverview.MyOverviewFragmentDirections
import nl.rijksoverheid.ctr.holder.ui.myoverview.items.MyOverviewInfoCardItem
import nl.rijksoverheid.ctr.shared.ext.navigateSafety
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Handles [MyOverviewInfoCardItem] actions
 */
interface MyOverviewFragmentInfoItemHandlerUtil {
    fun handleButtonClick(
        myOverviewFragment: MyOverviewFragment,
        infoItem: DashboardItem.InfoItem
    )

    fun handleDismiss(
        myOverviewFragment: MyOverviewFragment,
        infoCardItem: MyOverviewInfoCardItem,
        infoItem: DashboardItem.InfoItem
    )
}

class MyOverviewFragmentInfoItemHandlerUtilImpl(
    private val bottomSheetDialogUtil: BottomSheetDialogUtil
) : MyOverviewFragmentInfoItemHandlerUtil {

    /**
     * Handles the button click in the info card
     */
    override fun handleButtonClick(
        myOverviewFragment: MyOverviewFragment,
        infoItem: DashboardItem.InfoItem
    ) {
        when (infoItem) {
            is DashboardItem.InfoItem.ExtendedDomesticRecovery -> {
                bottomSheetDialogUtil.present(
                    myOverviewFragment.childFragmentManager,
                    BottomSheetData.TitleDescription(
                        title = myOverviewFragment.getString(R.string.extended_domestic_recovery_green_card_bottomsheet_title),
                        descriptionData = DescriptionData(
                            R.string.extended_domestic_recovery_green_card_bottomsheet_description,
                            htmlLinksEnabled = true
                        ),
                    )
                )
            }
            is DashboardItem.InfoItem.RecoveredDomesticRecovery -> {
                bottomSheetDialogUtil.present(
                    myOverviewFragment.childFragmentManager,
                    BottomSheetData.TitleDescription(
                        title = myOverviewFragment.getString(R.string.recovered_domestic_recovery_green_card_bottomsheet_title),
                        descriptionData = DescriptionData(R.string.recovered_domestic_recovery_green_card_bottomsheet_description),
                    )
                )
            }
            is DashboardItem.InfoItem.RefreshedEuVaccinations -> {
                bottomSheetDialogUtil.present(
                    myOverviewFragment.childFragmentManager,
                    BottomSheetData.TitleDescription(
                        title = myOverviewFragment.getString(R.string.refreshed_eu_items_title),
                        descriptionData = DescriptionData(
                            R.string.refreshed_eu_items_description,
                            htmlLinksEnabled = true
                        ),
                    )
                )
            }
            is DashboardItem.InfoItem.ExtendDomesticRecovery -> {
                myOverviewFragment.navigateSafety(
                    MyOverviewFragmentDirections.actionSyncGreenCards(
                        toolbarTitle = myOverviewFragment.getString(R.string.extend_domestic_recovery_green_card_toolbar_title),
                        title = myOverviewFragment.getString(R.string.extend_domestic_recovery_green_card_title),
                        description = myOverviewFragment.getString(R.string.extend_domestic_recovery_green_card_description),
                        button = myOverviewFragment.getString(R.string.extend_domestic_recovery_green_card_button)
                    )
                )
            }
            is DashboardItem.InfoItem.RecoverDomesticRecovery -> {
                myOverviewFragment.navigateSafety(
                    MyOverviewFragmentDirections.actionSyncGreenCards(
                        toolbarTitle = myOverviewFragment.getString(R.string.recover_domestic_recovery_green_card_toolbar_title),
                        title = myOverviewFragment.getString(R.string.recover_domestic_recovery_green_card_title),
                        description = myOverviewFragment.getString(R.string.recover_domestic_recovery_green_card_description),
                        button = myOverviewFragment.getString(R.string.recover_domestic_recovery_green_card_button)
                    )
                )
            }
            is DashboardItem.InfoItem.RefreshEuVaccinations -> {
                myOverviewFragment.navigateSafety(
                    MyOverviewFragmentDirections.actionSyncGreenCards(
                        toolbarTitle = myOverviewFragment.getString(R.string.refresh_eu_items_button),
                        title = myOverviewFragment.getString(R.string.refresh_eu_items_title),
                        description = myOverviewFragment.getString(R.string.refresh_eu_items_description),
                        button = myOverviewFragment.getString(R.string.refresh_eu_items_button)
                    )
                )
            }
            is DashboardItem.InfoItem.ConfigFreshnessWarning -> {
                bottomSheetDialogUtil.present(
                    myOverviewFragment.childFragmentManager,
                    BottomSheetData.TitleDescription(
                        title = myOverviewFragment.getString(R.string.config_warning_page_title),
                        descriptionData = DescriptionData(
                            htmlTextString = myOverviewFragment.getString(
                                R.string.config_warning_page_message,
                                OffsetDateTime.ofInstant(
                                    Instant.ofEpochSecond(infoItem.maxValidityDate),
                                    ZoneOffset.UTC
                                ).formatDayMonthTime(myOverviewFragment.requireContext())
                            ),
                            htmlLinksEnabled = true
                        )
                    )
                )
            }
            is DashboardItem.InfoItem.ClockDeviationItem -> addClockDeviation(
                myOverviewFragment
            )
            is DashboardItem.InfoItem.OriginInfoItem -> addOriginInfo(
                myOverviewFragment, infoItem
            )
            is DashboardItem.InfoItem.GreenCardExpiredItem -> {
                // NO OP, button is hidden
            }
        }
    }

    private fun addClockDeviation(
        myOverviewFragment: MyOverviewFragment
    ) {
        bottomSheetDialogUtil.present(
            myOverviewFragment.childFragmentManager, BottomSheetData.TitleDescription(
                title = myOverviewFragment.getString(R.string.clock_deviation_explanation_title),
                descriptionData = DescriptionData(
                    R.string.clock_deviation_explanation_description,
                    customLinkIntent = Intent(Settings.ACTION_DATE_SETTINGS)
                ),
            )
        )
    }

    private fun addOriginInfo(
        myOverviewFragment: MyOverviewFragment,
        item: DashboardItem.InfoItem.OriginInfoItem
    ) {
        when (item.greenCardType) {
            is GreenCardType.Domestic -> presentOriginInfoForDomesticQr(
                item.originType, myOverviewFragment
            )
            is GreenCardType.Eu -> presentOriginInfoForEuQr(
                item.originType, myOverviewFragment
            )
        }
    }

    private fun presentOriginInfoForEuQr(
        originType: OriginType,
        myOverviewFragment: MyOverviewFragment
    ) {
        bottomSheetDialogUtil.present(
            myOverviewFragment.childFragmentManager,
            data = when (originType) {
                is OriginType.Test -> {
                    BottomSheetData.TitleDescription(
                        title = myOverviewFragment.getString(R.string.my_overview_green_card_not_valid_title_test),
                        descriptionData = DescriptionData(R.string.my_overview_green_card_not_valid_eu_but_is_in_domestic_bottom_sheet_description_test),
                    )
                }
                is OriginType.Vaccination -> {
                    BottomSheetData.TitleDescription(
                        title = myOverviewFragment.getString(R.string.my_overview_green_card_not_valid_title_vaccination),
                        descriptionData = DescriptionData(R.string.my_overview_green_card_not_valid_eu_but_is_in_domestic_bottom_sheet_description_vaccination),
                    )
                }
                is OriginType.Recovery -> {
                    BottomSheetData.TitleDescription(
                        title = myOverviewFragment.getString(R.string.my_overview_green_card_not_valid_title_recovery),
                        descriptionData = DescriptionData(R.string.my_overview_green_card_not_valid_eu_but_is_in_domestic_bottom_sheet_description_recovery),
                    )
                }
            }
        )
    }

    private fun presentOriginInfoForDomesticQr(
        originType: OriginType,
        myOverviewFragment: MyOverviewFragment
    ) {
        val (title, description) = when (originType) {
            OriginType.Test -> Pair(
                myOverviewFragment.getString(R.string.my_overview_green_card_not_valid_title_test),
                R.string.my_overview_green_card_not_valid_domestic_but_is_in_eu_bottom_sheet_description_test
            )
            OriginType.Vaccination -> Pair(
                myOverviewFragment.getString(R.string.my_overview_green_card_not_valid_title_vaccination),
                R.string.my_overview_green_card_not_valid_domestic_but_is_in_eu_bottom_sheet_description_vaccination
            )
            OriginType.Recovery -> Pair(
                myOverviewFragment.getString(R.string.my_overview_green_card_not_valid_title_recovery),
                R.string.my_overview_green_card_not_valid_domestic_but_is_in_eu_bottom_sheet_description_recovery
            )
        }
        bottomSheetDialogUtil.present(
            myOverviewFragment.childFragmentManager,
            BottomSheetData.TitleDescription(
                title = title,
                descriptionData = DescriptionData(description, htmlLinksEnabled = true),
            )
        )
    }

    /**
     * Handles the dismiss button click in the info card
     */
    override fun handleDismiss(
        myOverviewFragment: MyOverviewFragment,
        infoCardItem: MyOverviewInfoCardItem,
        infoItem: DashboardItem.InfoItem
    ) {
        // Remove section from adapter
        myOverviewFragment.section.remove(infoCardItem)

        // Clear preference so it doesn't show again
        when (infoItem) {
            is DashboardItem.InfoItem.RefreshedEuVaccinations -> {
                myOverviewFragment.dashboardViewModel.dismissRefreshedEuVaccinationsInfoCard()
            }
            is DashboardItem.InfoItem.RecoveredDomesticRecovery -> {
                myOverviewFragment.dashboardViewModel.dismissRecoveredDomesticRecoveryInfoCard()
            }
            is DashboardItem.InfoItem.ExtendedDomesticRecovery -> {
                myOverviewFragment.dashboardViewModel.dismissExtendedDomesticRecoveryInfoCard()
            }
            is DashboardItem.InfoItem.GreenCardExpiredItem -> {
                myOverviewFragment.dashboardViewModel.removeGreenCard(infoItem.greenCard)
            }
            is DashboardItem.InfoItem.ClockDeviationItem,
            is DashboardItem.InfoItem.ConfigFreshnessWarning,
            is DashboardItem.InfoItem.ExtendDomesticRecovery,
            is DashboardItem.InfoItem.OriginInfoItem,
            is DashboardItem.InfoItem.RecoverDomesticRecovery,
            is DashboardItem.InfoItem.RefreshEuVaccinations -> {
                // NO OP, items can't be dismissed
            }
        }
    }
}