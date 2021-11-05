package nl.rijksoverheid.ctr.verifier.ui.scanner

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import nl.rijksoverheid.ctr.design.utils.BottomSheetData
import nl.rijksoverheid.ctr.design.utils.BottomSheetDialogUtil
import nl.rijksoverheid.ctr.shared.ext.navigateSafety
import nl.rijksoverheid.ctr.verifier.R
import nl.rijksoverheid.ctr.verifier.databinding.FragmentScanResultInvalidBinding
import nl.rijksoverheid.ctr.verifier.ui.scanner.models.ScanResultInvalidData
import nl.rijksoverheid.ctr.verifier.ui.scanner.utils.ScannerUtil
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
class ScanResultInvalidFragment : Fragment(R.layout.fragment_scan_result_invalid) {
    
    private val scannerUtil: ScannerUtil by inject()
    private val bottomSheetDialogUtil: BottomSheetDialogUtil by inject()

    private val autoCloseHandler = Handler(Looper.getMainLooper())
    private val autoCloseRunnable = Runnable {
        navigateSafety(R.id.nav_scan_result_invalid,
            ScanResultInvalidFragmentDirections.actionNavMain()
        )
    }

    private val args: ScanResultInvalidFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentScanResultInvalidBinding.bind(view)

        binding.toolbar.setNavigationOnClickListener {
            navigateSafety(ScanResultInvalidFragmentDirections.actionNavMain())
        }

        when (args.invalidData) {
            is ScanResultInvalidData.Invalid -> {
                binding.title.text = getString(R.string.scan_result_european_nl_invalid_title)
                binding.buttonExplanation.visibility = View.INVISIBLE
            }
            is ScanResultInvalidData.Error -> {
                binding.buttonExplanation.setOnClickListener {
                    bottomSheetDialogUtil.present(childFragmentManager, BottomSheetData.TitleDescription(
                        title = getString(R.string.scan_result_invalid_reason_title),
                        applyOnDescription = {
                            it.setHtmlText(R.string.scan_result_invalid_reason_description)
                        }
                    ))
                }
            }
        }

        binding.buttonNext.setOnClickListener {
            scannerUtil.launchScanner(requireActivity())
        }
    }

    override fun onResume() {
        super.onResume()
        val autoCloseDuration = TimeUnit.SECONDS.toMillis(5)
        autoCloseHandler.postDelayed(autoCloseRunnable, autoCloseDuration)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        autoCloseHandler.removeCallbacks(autoCloseRunnable)
    }
}
