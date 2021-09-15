package nl.rijksoverheid.ctr.holder.ui.create_qr.util

import android.app.Application
import android.os.Build
import android.os.Parcelable
import android.text.TextUtils
import kotlinx.parcelize.Parcelize
import nl.rijksoverheid.ctr.design.ext.formatDayMonthYear
import nl.rijksoverheid.ctr.design.ext.formatDayMonthYearNumerical
import nl.rijksoverheid.ctr.holder.R
import nl.rijksoverheid.ctr.holder.persistence.CachedAppConfigUseCase
import nl.rijksoverheid.ctr.holder.ui.create_qr.models.*
import nl.rijksoverheid.ctr.shared.ext.getStringOrNull
import nl.rijksoverheid.ctr.shared.models.PersonalDetails
import org.json.JSONObject
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*

interface InfoScreenUtil {
    fun getForRemoteTestResult2(
        result: RemoteTestResult2.Result,
        personalDetails: PersonalDetails,
        testDate: String
    ): InfoScreen

    fun getForNegativeTest(
        event: RemoteEventNegativeTest,
        fullName: String,
        testDate: String,
        birthDate: String
    ): InfoScreen

    fun getForVaccination(
        event: RemoteEventVaccination,
        fullName: String,
        birthDate: String,
        providerIdentifier: String,
    ): InfoScreen

    fun getForPositiveTest(
        event: RemoteEventPositiveTest,
        testDate: String,
        fullName: String,
        birthDate: String
    ): InfoScreen

    fun getForRecovery(
        event: RemoteEventRecovery,
        testDate: String,
        fullName: String,
        birthDate: String
    ): InfoScreen

    fun getForDomesticQr(personalDetails: PersonalDetails): InfoScreen
    fun getForEuropeanTestQr(readEuropeanCredential: JSONObject): InfoScreen
    fun getForEuropeanVaccinationQr(readEuropeanCredential: JSONObject): InfoScreen
    fun getForEuropeanRecoveryQr(readEuropeanCredential: JSONObject): InfoScreen

    fun getCountry(countryCode: String?, currentLocale: Locale?): String
}

class InfoScreenUtilImpl(
    private val application: Application,
    private val vaccinationInfoScreenUtil: VaccinationInfoScreenUtil,
    cachedAppConfigUseCase: CachedAppConfigUseCase
) : InfoScreenUtil {

    private val holderConfig = cachedAppConfigUseCase.getCachedAppConfig()

    override fun getForRemoteTestResult2(
        result: RemoteTestResult2.Result,
        personalDetails: PersonalDetails,
        testDate: String
    ): InfoScreen {
        val title = application.getString(R.string.your_test_result_explanation_toolbar_title)
        val description = (TextUtils.concat(
            application.getString(R.string.your_test_result_3_0_explanation_description_header),
            "<br/><br/>",
            createdLine(
                application.getString(R.string.your_test_result_3_0_explanation_description_your_details),
                "${personalDetails.firstNameInitial} ${personalDetails.lastNameInitial} ${personalDetails.birthDay} ${personalDetails.birthMonth}"
            ),
            createdLine(
                application.getString(R.string.your_test_result_3_0_explanation_description_test_type),
                result.testType,
            ),
            createdLine(
                application.getString(R.string.your_test_result_3_0_explanation_description_test_date),
                testDate
            ),
            createdLine(
                application.getString(R.string.your_test_result_3_0_explanation_description_test_result),
                application.getString(R.string.your_test_result_explanation_negative_test_result)
            ),
            createdLine(
                application.getString(R.string.your_test_result_3_0_explanation_description_unique_identifier),
                result.unique
            )
        ) as String)

        return InfoScreen(
            title = title,
            description = description
        )
    }

    override fun getForNegativeTest(
        event: RemoteEventNegativeTest,
        fullName: String,
        testDate: String,
        birthDate: String
    ): InfoScreen {

        val testType = holderConfig.euTestTypes.firstOrNull {
            it.code == event.negativeTest?.type
        }?.name ?: event.negativeTest?.type ?: ""

        val testName = event.negativeTest?.name ?: ""

        val testLocation = event.negativeTest?.facility ?: ""

        val testManufacturer =
            holderConfig.euTestManufacturers.firstOrNull {
                it.code == event.negativeTest?.manufacturer
            }?.name ?: event.negativeTest?.manufacturer ?: ""

        val unique = event.unique ?: ""

        val title = application.getString(R.string.your_test_result_explanation_toolbar_title)
        val description = (TextUtils.concat(
            application.getString(R.string.your_test_result_3_0_explanation_description_header),
            "<br/><br/>",
            createdLine(
                application.getString(R.string.your_test_result_3_0_explanation_description_name),
                fullName
            ),
            createdLine(
                application.getString(R.string.your_test_result_3_0_explanation_description_date_of_birth),
                birthDate,
                isOptional = true
            ),
            createdLine(
                application.getString(R.string.your_test_result_3_0_explanation_description_test_type),
                testType
            ),
            createdLine(
                application.getString(R.string.your_test_result_3_0_explanation_description_test_name),
                testName
            ),
            createdLine(
                application.getString(R.string.your_test_result_3_0_explanation_description_test_date),
                testDate
            ),
            createdLine(
                application.getString(R.string.your_test_result_3_0_explanation_description_test_result),
                application.getString(R.string.your_test_result_explanation_negative_test_result)
            ),
            createdLine(
                application.getString(R.string.your_test_result_3_0_explanation_description_test_location),
                testLocation
            ),
            createdLine(
                application.getString(R.string.your_test_result_3_0_explanation_description_test_manufacturer),
                testManufacturer
            ),
            createdLine(
                application.getString(R.string.your_test_result_3_0_explanation_description_unique_identifier),
                unique
            )
        ) as String)

        return InfoScreen(
            title = title,
            description = description
        )
    }

    override fun getForVaccination(
        event: RemoteEventVaccination,
        fullName: String,
        birthDate: String,
        providerIdentifier: String,
    ): InfoScreen {
        return vaccinationInfoScreenUtil.getForVaccination(
            event,
            fullName,
            birthDate,
            providerIdentifier
        )
    }

    override fun getForPositiveTest(
        event: RemoteEventPositiveTest,
        testDate: String,
        fullName: String,
        birthDate: String
    ): InfoScreen {

        val testType = holderConfig.euTestTypes.firstOrNull {
            it.code == event.positiveTest?.type
        }?.name ?: event.positiveTest?.type ?: ""

        val testName = event.positiveTest?.name ?: ""

        val testLocation = event.positiveTest?.facility ?: ""

        val testManufacturer =
            holderConfig.euTestManufacturers.firstOrNull {
                it.code == event.positiveTest?.manufacturer
            }?.name ?: event.positiveTest?.manufacturer ?: ""

        val unique = event.unique ?: ""

        val title = application.getString(R.string.your_test_result_explanation_toolbar_title)
        val description = (TextUtils.concat(
            application.getString(R.string.your_test_result_3_0_explanation_description_header),
            "<br/><br/>",
            createdLine(
                application.getString(R.string.your_test_result_3_0_explanation_description_name),
                fullName
            ),
            createdLine(
                application.getString(R.string.your_test_result_3_0_explanation_description_date_of_birth),
                birthDate,
                isOptional = true
            ),
            createdLine(
                application.getString(R.string.your_test_result_3_0_explanation_description_test_type),
                testType
            ),
            createdLine(
                application.getString(R.string.your_test_result_3_0_explanation_description_test_name),
                testName
            ),
            createdLine(
                application.getString(R.string.your_test_result_3_0_explanation_description_test_date),
                testDate
            ),
            createdLine(
                application.getString(R.string.your_test_result_3_0_explanation_description_test_result),
                application.getString(R.string.your_test_result_explanation_positive_test_result)
            ),
            createdLine(
                application.getString(R.string.your_test_result_3_0_explanation_description_test_location),
                testLocation
            ),
            createdLine(
                application.getString(R.string.your_test_result_3_0_explanation_description_test_manufacturer),
                testManufacturer
            ),
            createdLine(
                application.getString(R.string.your_test_result_3_0_explanation_description_unique_identifier),
                unique
            )
        ) as String)

        return InfoScreen(
            title = title,
            description = description
        )
    }

    override fun getForRecovery(
        event: RemoteEventRecovery,
        testDate: String,
        fullName: String,
        birthDate: String
    ): InfoScreen {

        val validFromDate = event.recovery?.validFrom?.formatDayMonthYear() ?: ""
        val validUntilDate = event.recovery?.validUntil?.formatDayMonthYear() ?: ""

        val title = application.getString(R.string.your_test_result_explanation_toolbar_title)
        val description = (TextUtils.concat(
            application.getString(R.string.recovery_explanation_description_header),
            "<br/><br/>",
            createdLine(
                application.getString(R.string.recovery_explanation_description_name),
                fullName
            ),
            createdLine(
                application.getString(R.string.recovery_explanation_description_birth_date),
                birthDate,
                isOptional = true
            ),
            createdLine(
                application.getString(R.string.recovery_explanation_description_test_date),
                testDate
            ),
            createdLine(
                application.getString(R.string.recovery_explanation_description_valid_from),
                validFromDate
            ),
            createdLine(
                application.getString(R.string.recovery_explanation_description_valid_until),
                validUntilDate
            ),
            createdLine(
                application.getString(R.string.recovery_explanation_description_unique_test_identifier),
                event.unique
            ),
        ) as String)

        return InfoScreen(
            title = title,
            description = description
        )
    }

    private fun createdLine(
        name: String,
        nameAnswer: String,
        isOptional: Boolean = false
    ): String {
        return if (isOptional && nameAnswer.isEmpty()) "" else "$name <b>$nameAnswer</b><br/>"
    }


    override fun getForDomesticQr(personalDetails: PersonalDetails): InfoScreen {
        val title = application.getString(R.string.qr_explanation_title_domestic)
        val description = application.getString(
            R.string.qr_explanation_description_domestic,
            "${personalDetails.firstNameInitial} ${personalDetails.lastNameInitial} ${personalDetails.birthDay} ${personalDetails.birthMonth}"
        )

        return InfoScreen(
            title = title,
            description = description
        )
    }

    override fun getForEuropeanTestQr(readEuropeanCredential: JSONObject): InfoScreen {
        val dcc = readEuropeanCredential.optJSONObject("dcc")
        val test = dcc.getJSONArray("t").optJSONObject(0)

        val title = application.getString(R.string.qr_explanation_title_eu)

        val fullName = "${dcc.optJSONObject("nam").getStringOrNull("fn")}, ${
            dcc.optJSONObject("nam").getStringOrNull("gn")
        }"

        val birthDate = dcc.getStringOrNull("dob")?.let { birthDate ->
            try {
                LocalDate.parse(birthDate, DateTimeFormatter.ISO_DATE).formatDayMonthYearNumerical()
            } catch (e: Exception) {
                ""
            }
        } ?: ""

        val disease = application.getString(R.string.your_vaccination_explanation_covid_19_answer)

        val testType = holderConfig.euTestTypes.firstOrNull {
            it.code == test.getStringOrNull("tt")
        }?.name ?: test.getStringOrNull("tt") ?: ""

        val testName = test.getStringOrNull("nm") ?: ""

        val testDate = test.getStringOrNull("sc")?.let {
            try {
                OffsetDateTime.parse(it, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    .toLocalDate().formatDayMonthYearNumerical()
            } catch (e: Exception) {
                ""
            }
        } ?: ""

        val testResult =
            application.getString(R.string.your_test_result_explanation_negative_test_result)

        val testLocation = test.getStringOrNull("tc") ?: ""

        val manufacturer =
            holderConfig.euManufacturers.firstOrNull {
                it.code == test.getStringOrNull("ma")
            }?.name ?: test.getStringOrNull("ma") ?: ""

        val testCountry = getCountry(test.getStringOrNull("co"), getCurrentLocale())

        val issuerValue = test.getStringOrNull("is")
        val issuer = if (issuerValue == issuerVWS) {
            application.getString(R.string.qr_explanation_certificate_issuer)
        } else {
            issuerValue
        }

        val uniqueCode = test.getStringOrNull("ci")

        return InfoScreen(
            title = title,
            description = (TextUtils.concat(
                application.getString(R.string.qr_explanation_description_eu_test_header),
                "<br/><br/>",
                application.getString(R.string.qr_explanation_description_eu_test_name),
                createQrAnswer(fullName),
                application.getString(R.string.qr_explanation_description_eu_test_birth_date),
                createQrAnswer(birthDate),
                application.getString(R.string.qr_explanation_description_eu_test_disease),
                createQrAnswer(disease),
                application.getString(R.string.qr_explanation_description_eu_test_test_type),
                createQrAnswer(testType),
                application.getString(R.string.qr_explanation_description_eu_test_test_name),
                createQrAnswer(testName),
                application.getString(R.string.qr_explanation_description_eu_test_test_date),
                createQrAnswer(testDate),
                application.getString(R.string.qr_explanation_description_eu_test_test_result),
                createQrAnswer(testResult),
                application.getString(R.string.qr_explanation_description_eu_test_test_centre),
                createQrAnswer(testLocation),
                application.getString(R.string.qr_explanation_description_eu_test_manufacturer),
                createQrAnswer(manufacturer),
                application.getString(R.string.qr_explanation_description_eu_test_test_country),
                createQrAnswer(testCountry),
                application.getString(R.string.qr_explanation_description_eu_test_issuer),
                createQrAnswer(issuer ?: ""),
                application.getString(R.string.qr_explanation_description_eu_test_certificate_identifier),
                createQrAnswer(uniqueCode ?: ""),
                application.getString(R.string.qr_explanation_description_eu_test_footer),
            ) as String)
        )
    }

    private fun getCurrentLocale(): Locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        application.resources.configuration.locales[0]
    } else {
        application.resources.configuration.locale
    }

    override fun getCountry(
        countryCode: String?,
        currentLocale: Locale?
    ): String = if (countryCode != null) {
        val localeIsNL = currentLocale?.country == "NL"
        val countryIsNL = countryCode == "NL"
        val countryNameInDutch = Locale("", countryCode).getDisplayCountry(Locale("nl"))
        val countryNameInEnglish = Locale("", countryCode).getDisplayCountry(Locale("en"))

        // GetDisplayCountry returns country for "NL" as "Netherlands" instead of "The Netherlands"
        if (localeIsNL && countryIsNL) {
            "$countryNameInDutch / The $countryNameInEnglish"
        } else if (localeIsNL) {
            "$countryNameInDutch / $countryNameInEnglish"
        } else {
            countryNameInEnglish
        }
    } else {
        ""
    }

    override fun getForEuropeanVaccinationQr(readEuropeanCredential: JSONObject): InfoScreen {
        val dcc = readEuropeanCredential.optJSONObject("dcc")
        val vaccination = dcc.getJSONArray("v").optJSONObject(0)

        val title = application.getString(R.string.qr_explanation_title_eu)

        val fullName = "${dcc.optJSONObject("nam").getStringOrNull("fn")}, ${
            dcc.optJSONObject("nam").getStringOrNull("gn")
        }"

        val birthDate = dcc.getStringOrNull("dob")?.let { birthDate ->
            try {
                LocalDate.parse(birthDate, DateTimeFormatter.ISO_DATE).formatDayMonthYearNumerical()
            } catch (e: DateTimeParseException) {
                // Check if date has removed content, if so return year or string only
                if (birthDate.contains("XX")) {
                    // Retrieve birth year only
                    birthDate.split("-").first()
                } else birthDate
            } catch (e: Exception) {
                ""
            }
        } ?: ""

        val disease = application.getString(R.string.your_vaccination_explanation_covid_19_answer)

        val vaccin = holderConfig.euBrands.firstOrNull {
            it.code == vaccination.getStringOrNull("mp")
        }?.name ?: vaccination.getStringOrNull("mp") ?: ""

        val vaccinType = holderConfig.euVaccinations.firstOrNull {
            it.code == vaccination.getStringOrNull("vp")
        }?.name ?: vaccination.getStringOrNull("vp") ?: ""

        val manufacturer =
            holderConfig.euManufacturers.firstOrNull {
                it.code == vaccination.getStringOrNull("ma")
            }?.name ?: vaccination.getStringOrNull("ma") ?: ""

        val doses =
            if (vaccination.getStringOrNull("dn") != null && vaccination.getStringOrNull("sd") != null) {
                application.getString(
                    R.string.your_vaccination_explanation_doses_answer,
/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
                    vaccination.getStringOrNull("dn"),
                    vaccination.getStringOrNull("sd")
                )
            } else ""

        val vaccinationDate = vaccination.getStringOrNull("dt")?.let { vaccinationDate ->
            try {
                LocalDate.parse(vaccinationDate, DateTimeFormatter.ISO_DATE)
                    .formatDayMonthYearNumerical()
            } catch (e: Exception) {
                ""
            }
        } ?: ""

        val countryCode = vaccination.getStringOrNull("co")
        val vaccinationCountry = getCountry(countryCode, getCurrentLocale())

        val issuerValue = vaccination.getStringOrNull("is")
        val issuer = if (issuerValue == issuerVWS) {
            application.getString(R.string.qr_explanation_certificate_issuer)
        } else {
            issuerValue
        }

        val uniqueCode = vaccination.getStringOrNull("ci")

        return InfoScreen(
            title = title,
            description = (TextUtils.concat(
                application.getString(R.string.qr_explanation_description_eu_vaccination_header),
                "<br/><br/>",
                application.getString(R.string.qr_explanation_description_eu_vaccination_name),
                createQrAnswer(fullName),
                application.getString(R.string.qr_explanation_description_eu_vaccination_birth_date),
                createQrAnswer(birthDate),
                application.getString(R.string.qr_explanation_description_eu_vaccination_disease),
                createQrAnswer(disease),
                application.getString(R.string.qr_explanation_description_eu_vaccination_vaccine),
                createQrAnswer(vaccin),
                application.getString(R.string.qr_explanation_description_eu_vaccination_vaccine_type),
                createQrAnswer(vaccinType),
                application.getString(R.string.qr_explanation_description_eu_vaccination_producer),
                createQrAnswer(manufacturer),
                application.getString(R.string.qr_explanation_description_eu_vaccination_doses),
                createQrAnswer(doses),
                application.getString(R.string.qr_explanation_description_eu_vaccination_vaccination_date),
                createQrAnswer(vaccinationDate),
                application.getString(R.string.qr_explanation_description_eu_vaccination_vaccinated_in),
                createQrAnswer(vaccinationCountry),
                application.getString(R.string.qr_explanation_description_eu_vaccination_certificate_issuer),
                createQrAnswer(issuer ?: ""),
                application.getString(R.string.qr_explanation_description_eu_vaccination_unique_certificate),
                createQrAnswer(uniqueCode ?: ""),
                application.getString(R.string.qr_explanation_description_eu_vaccination_footer),
            ) as String)
        )
    }

    override fun getForEuropeanRecoveryQr(readEuropeanCredential: JSONObject): InfoScreen {
        val dcc = readEuropeanCredential.optJSONObject("dcc")
        val recovery = dcc.getJSONArray("r").optJSONObject(0)

        val title = application.getString(R.string.qr_explanation_title_eu)

        val fullName = "${dcc.optJSONObject("nam").getStringOrNull("fn")}, ${
            dcc.optJSONObject("nam").getStringOrNull("gn")
        }"

        val birthDate = dcc.getStringOrNull("dob")?.let { birthDate ->
            try {
                LocalDate.parse(birthDate, DateTimeFormatter.ISO_DATE).formatDayMonthYearNumerical()
            } catch (e: Exception) {
                ""
            }
        } ?: ""

        val disease = application.getString(R.string.your_vaccination_explanation_covid_19_answer)

        val testDate = recovery.getStringOrNull("fr")?.let { testDate ->
            try {
                LocalDate.parse(testDate, DateTimeFormatter.ISO_DATE).formatDayMonthYearNumerical()
            } catch (e: Exception) {
                ""
            }
        } ?: ""

        val country = getCountry(recovery.getStringOrNull("co"), getCurrentLocale())

        val producer = recovery.getStringOrNull("is")

        val validFromDate = recovery.getStringOrNull("df")?.let { testDate ->
            try {
                LocalDate.parse(testDate, DateTimeFormatter.ISO_DATE).formatDayMonthYearNumerical()
            } catch (e: Exception) {
                ""
            }
        } ?: ""

        val validUntilDate = recovery.getStringOrNull("du")?.let { testDate ->
            try {
                LocalDate.parse(testDate, DateTimeFormatter.ISO_DATE).formatDayMonthYearNumerical()
            } catch (e: Exception) {
                ""
            }
        } ?: ""

        val uniqueCode = recovery.getStringOrNull("ci")

        return InfoScreen(
            title = title,
            description = (TextUtils.concat(
                application.getString(R.string.qr_explanation_description_eu_recovery_header),
                "<br/><br/>",
                application.getString(R.string.qr_explanation_description_eu_recovery_name),
                createQrAnswer(fullName),
                application.getString(R.string.qr_explanation_description_eu_recovery_birth_date),
                createQrAnswer(birthDate),
                application.getString(R.string.qr_explanation_description_eu_recovery_disease),
                createQrAnswer(disease),
                application.getString(R.string.qr_explanation_description_eu_recovery_test_date),
                createQrAnswer(testDate),
                application.getString(R.string.qr_explanation_description_eu_recovery_country),
                createQrAnswer(country),
                application.getString(R.string.qr_explanation_description_eu_recovery_producer),
                createQrAnswer(producer ?: ""),
                application.getString(R.string.qr_explanation_description_eu_recovery_valid_from_date),
                createQrAnswer(validFromDate),
                application.getString(R.string.qr_explanation_description_eu_recovery_valid_until_date),
                createQrAnswer(validUntilDate),
                application.getString(R.string.qr_explanation_description_eu_recovery_unique_code),
                createQrAnswer(uniqueCode ?: ""),
                application.getString(R.string.qr_explanation_description_eu_recovery_footer)
            ) as String)
        )
    }

    private fun createQrAnswer(answer: String): String =
        "<br/><b>$answer</b><br/><br/>"

    companion object {
        private const val issuerVWS = "Ministry of Health Welfare and Sport"
    }

}

@Parcelize
data class InfoScreen(
    val title: String,
    val description: String
) : Parcelable