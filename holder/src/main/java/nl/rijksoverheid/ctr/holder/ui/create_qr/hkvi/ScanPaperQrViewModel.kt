package nl.rijksoverheid.ctr.holder.ui.create_qr.hkvi

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import nl.rijksoverheid.ctr.shared.livedata.Event

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
abstract class ScanPaperQrViewModel : ViewModel() {
    val loading: LiveData<Event<Boolean>> = MutableLiveData()

    abstract fun onQrScanned(qrCode: String)
}

class ScanPaperQrViewModelImpl(
    private val getEventFromQrUseCase: GetEventFromQrUseCase
): ScanPaperQrViewModel() {

    override fun onQrScanned(qrCode: String) {
        getEventFromQrUseCase.get(qrCode)
    }
}