package nl.rijksoverheid.ctr.holder.myoverview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import nl.rijksoverheid.ctr.holder.R
import nl.rijksoverheid.ctr.holder.databinding.DialogQrCodeBinding
import nl.rijksoverheid.ctr.shared.ext.formatDateShort
import nl.rijksoverheid.ctr.shared.livedata.EventObserver
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId


/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
class QrCodeFragment : DialogFragment() {

    private lateinit var binding: DialogQrCodeBinding

    private val localTestResultViewModel: LocalTestResultViewModel by sharedViewModel()
    private val qrCodeViewModel: QrCodeViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(
            STYLE_NORMAL,
            R.style.AppTheme_Dialog_FullScreen
        )
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogQrCodeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val params = dialog?.window?.attributes
        params?.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
        dialog?.window?.attributes = params

        qrCodeViewModel.qrCodeLiveData.observe(viewLifecycleOwner, EventObserver {
            binding.image.setImageBitmap(it.qrCode)
            binding.loading.visibility = View.GONE
            binding.content.visibility = View.VISIBLE
        })

        val localTestResult = localTestResultViewModel.retrievedLocalTestResult
        if (localTestResult == null) {
            // No credentials in cache, go back to overview
            findNavController().popBackStack()
        } else {
            binding.title.text = OffsetDateTime.ofInstant(
                Instant.ofEpochMilli(localTestResult.dateOfBirthMillis),
                ZoneId.of("UTC")
            ).formatDateShort()

            binding.image.doOnPreDraw {
                lifecycleScope.launchWhenResumed {
                    qrCodeViewModel.generateQrCode(
                        localTestResult = localTestResult,
                        qrCodeSize = binding.image.height
                    )
                }
            }
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }
}
