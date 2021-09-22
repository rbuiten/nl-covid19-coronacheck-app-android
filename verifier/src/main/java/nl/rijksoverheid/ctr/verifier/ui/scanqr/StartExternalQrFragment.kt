package nl.rijksoverheid.ctr.verifier.ui.scanqr

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import nl.rijksoverheid.ctr.verifier.R
import nl.rijksoverheid.ctr.verifier.VerifierMainActivity
import nl.rijksoverheid.ctr.verifier.databinding.FragmentScanExternalQrBinding

class StartExternalQrFragment : Fragment(R.layout.fragment_scan_external_qr) {

    private lateinit var binding: FragmentScanExternalQrBinding

    private val mainActivity: VerifierMainActivity? by lazy {
        requireActivity() as? VerifierMainActivity
    }

    override fun onPause() {
        mainActivity?.statusListener = null
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        mainActivity?.statusListener = { connected: Boolean, message: String ->
            setListeners(connected, message)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentScanExternalQrBinding.bind(view)

        setListeners(mainActivity?.hasConnectionWithExternalBarcode() == true, null)
    }

    private fun setListeners(connected: Boolean, message: String?) {
        binding.textStatus.text = message ?: "Niet verbonden"

        if (connected) {
            binding.textStatus.text = "Verbonden"

            binding.textStatus.setOnClickListener {
                mainActivity?.disconnectFromExternalBarcode()
            }
        } else {

            binding.textStatus.setOnClickListener {
                mainActivity?.connectWithExternalBarcode()
            }
        }
    }
}
