package nl.rijksoverheid.ctr.holder

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import nl.rijksoverheid.ctr.shared.models.Flow

sealed class HolderFlow(code: Int) : Flow(code), Parcelable {

    @Parcelize
    object Startup: HolderFlow(0), Parcelable

    @Parcelize
    object CommercialTest: HolderFlow(1), Parcelable

    @Parcelize
    object Vaccination: HolderFlow(2), Parcelable

    @Parcelize
    object Recovery: HolderFlow(3), Parcelable

    @Parcelize
    object DigidTest: HolderFlow(4), Parcelable

    @Parcelize
    object HkviScan: HolderFlow(5), Parcelable

    @Parcelize
    object SyncGreenCards: HolderFlow(7), Parcelable

    @Parcelize
    object PositiveTest: HolderFlow(8), Parcelable

    @Parcelize
    object VaccinationAssessment: HolderFlow(9), Parcelable

    @Parcelize
    object VaccinationBesIslands: HolderFlow(10), Parcelable
}