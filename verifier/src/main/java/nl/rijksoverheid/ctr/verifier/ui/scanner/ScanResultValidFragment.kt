package nl.rijksoverheid.ctr.verifier.ui.scanner

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import nl.rijksoverheid.ctr.shared.ext.findNavControllerSafety
import nl.rijksoverheid.ctr.shared.utils.PersonalDetailsUtil
import nl.rijksoverheid.ctr.verifier.BuildConfig
import nl.rijksoverheid.ctr.verifier.R
import nl.rijksoverheid.ctr.verifier.databinding.FragmentScanResultValidBinding
import nl.rijksoverheid.ctr.verifier.ui.scanner.models.ScanResultValidData
import nl.rijksoverheid.ctr.verifier.ui.scanner.utils.ScannerUtil
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit


class ScanResultValidFragment : Fragment(R.layout.fragment_scan_result_valid) {

    private var _binding: FragmentScanResultValidBinding? = null
    private val binding get() = _binding!!

    private val args: ScanResultValidFragmentArgs by navArgs()
    private val scannerUtil: ScannerUtil by inject()
    private val personalDetailsUtil: PersonalDetailsUtil by inject()

    private val autoCloseHandler = Handler(Looper.getMainLooper())
    private val autoCloseRunnable = Runnable {
        findNavControllerSafety(R.id.nav_scan_result_valid)?.navigate(
            ScanResultValidFragmentDirections.actionNavMain()
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentScanResultValidBinding.bind(view)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigate(ScanResultValidFragmentDirections.actionNavMain())
        }

        when (args.validData) {
            is ScanResultValidData.Demo -> {
                binding.root.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.grey_2
                    )
                )
            }
            is ScanResultValidData.Valid -> {
                binding.root.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.secondary_green
                    )
                )
            }
        }

        binding.personalDetails.icon.setOnClickListener {
            findNavController().navigate(ScanResultValidFragmentDirections.actionShowValidExplanation())
        }

        binding.personalDetails.bottom.setButtonClick {
            scannerUtil.launchScanner(requireActivity())
        }

        // If you touch the screen before personal details screen animation started, immediately show personal details screen without animating
        binding.personalDetails.scroll.setOnTouchListener { _, _ ->
            binding.personalDetails.root.animate().cancel()
            binding.personalDetails.root.alpha = 1f
            false
        }

        binding.personalDetails.root.alpha = 0f
        binding.personalDetails.root.animate().alpha(1f).setDuration(500).setStartDelay(800)
            .start()

        presentPersonalDetails()
    }

    private fun presentPersonalDetails() {
        val testResultAttributes = args.validData.verifiedQr.testResultAttributes
        val personalDetails = personalDetailsUtil.getPersonalDetails(
            testResultAttributes.firstNameInitial,
            testResultAttributes.lastNameInitial,
            testResultAttributes.birthDay,
            testResultAttributes.birthMonth,
            includeBirthMonthNumber = true
        )
        binding.personalDetails.lastNameInitial.text = personalDetails.lastNameInitial
        binding.personalDetails.firstNameInitial.text = personalDetails.firstNameInitial
        binding.personalDetails.birthMonth.text = personalDetails.birthMonth
        binding.personalDetails.birthDay.text = personalDetails.birthDay
    }

    override fun onResume() {
        super.onResume()
        val autoCloseDuration =
            if (BuildConfig.FLAVOR == "tst") TimeUnit.SECONDS.toMillis(10) else TimeUnit.MINUTES.toMillis(
                3
            )
        autoCloseHandler.postDelayed(autoCloseRunnable, autoCloseDuration)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        autoCloseHandler.removeCallbacks(autoCloseRunnable)
    }
}
