package nl.rijksoverheid.ctr.verifier

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.ProbeTable
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import nl.rijksoverheid.ctr.appconfig.AppConfigViewModel
import nl.rijksoverheid.ctr.appconfig.AppStatusFragment
import nl.rijksoverheid.ctr.appconfig.models.AppStatus
import nl.rijksoverheid.ctr.design.utils.DialogUtil
import nl.rijksoverheid.ctr.introduction.IntroductionFragment
import nl.rijksoverheid.ctr.introduction.IntroductionViewModel
import nl.rijksoverheid.ctr.introduction.ui.status.models.IntroductionStatus
import nl.rijksoverheid.ctr.shared.MobileCoreWrapper
import nl.rijksoverheid.ctr.shared.livedata.EventObserver
import nl.rijksoverheid.ctr.shared.utils.IntentUtil
import nl.rijksoverheid.ctr.verifier.databinding.ActivityMainBinding
import nl.rijksoverheid.ctr.verifier.ui.scanner.VerifierQrScannerFragmentArgs
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.io.IOException
import java.nio.charset.StandardCharsets


/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
class VerifierMainActivity : AppCompatActivity(), SerialInputOutputManager.Listener {

    private val INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB"

    private var deviceId = 0
    private var portNum = 0
    private var baudRate = 0
    private var withIoManager = false

    private var broadcastReceiver: BroadcastReceiver? = null
    private var mainLooper: Handler? = null

    private var usbIoManager: SerialInputOutputManager? = null
    private var usbSerialPort: UsbSerialPort? = null
    private var usbPermission = UsbPermission.Unknown
    private var connected = false

    private val introductionViewModel: IntroductionViewModel by viewModel()
    private val appConfigViewModel: AppConfigViewModel by viewModel()
    private val mobileCoreWrapper: MobileCoreWrapper by inject()
    private val dialogUtil: DialogUtil by inject()
    private val intentUtil: IntentUtil by inject()

    private var isFreshStart: Boolean = true // track if this is a fresh start of the app

    var returnUri: String? = null // return uri to external app given as argument from deeplink
    private var hasHandledDeeplink: Boolean = false

    private lateinit var binding: ActivityMainBinding
    private var qrCode = ""
    private var loading = false

    var statusListener: ((Boolean, String) -> Unit)? = null

    init {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (INTENT_ACTION_GRANT_USB == intent.action) {
                    usbPermission = if (intent.getBooleanExtra(
                            UsbManager.EXTRA_PERMISSION_GRANTED,
                            false
                        )
                    ) UsbPermission.Granted else UsbPermission.Denied
                    connect()
                }
            }
        }
        mainLooper = Handler(Looper.getMainLooper())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setProductionFlags()

        observeStatuses()
    }

    override fun onStart() {
        super.onStart()
        if (isIntroductionFinished()) {
            if (isFreshStart) {
                // Force retrieval of config once on startup for clock deviation checks
                appConfigViewModel.refresh(mobileCoreWrapper, force = true)
            } else {
                // Only get app config on every app foreground when introduction is finished and the app has already started
                appConfigViewModel.refresh(mobileCoreWrapper)
            }
            isFreshStart = false
        }
    }

    private fun observeStatuses() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.main_nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        introductionViewModel.introductionStatusLiveData.observe(this, EventObserver {
            navController.navigate(R.id.action_introduction, IntroductionFragment.getBundle(it))
        })

        appConfigViewModel.appStatusLiveData.observe(this, {
            handleAppStatus(it, navController)
        })

        navController.addOnDestinationChangedListener { _, destination, arguments ->
            if (destination.id == R.id.nav_main) {
                // Persist deeplink return uri in case it's not used immediately because of onboarding
                arguments?.getString("returnUri")?.let { returnUri = it }
                navigateDeeplink(navController)
            }

            // verifier can stay active for a long time, so it is not sufficient
            // to try to refresh the config only every time the app resumes.
            // We do track if the app was recently (re)started to avoid double config calls
            if (!isFreshStart && isIntroductionFinished()) {
                appConfigViewModel.refresh(mobileCoreWrapper)
            } else {
                isFreshStart = false
            }
        }
    }

    private fun navigateDeeplink(navController: NavController) {
        if (returnUri != null && !hasHandledDeeplink && isIntroductionFinished()) {
            navController.navigate(RootNavDirections.actionScanner())
        }
        hasHandledDeeplink = true
    }

    private fun isIntroductionFinished() =
        introductionViewModel.getIntroductionStatus() is IntroductionStatus.IntroductionFinished

    private fun handleAppStatus(
        appStatus: AppStatus,
        navController: NavController
    ) {
        if (appStatus is AppStatus.UpdateRecommended) {
            showRecommendedUpdateDialog()
            return
        }

        if (appStatus !is AppStatus.NoActionRequired) navigateToAppStatus(appStatus, navController)
    }

    private fun navigateToAppStatus(
        appStatus: AppStatus,
        navController: NavController
    ) {
        val bundle = bundleOf(AppStatusFragment.EXTRA_APP_STATUS to appStatus)
        navController.navigate(R.id.action_app_status, bundle)
    }

    private fun showRecommendedUpdateDialog() {
        dialogUtil.presentDialog(
            context = this,
            title = R.string.app_status_update_recommended_title,
            message = getString(R.string.app_status_update_recommended_message),
            positiveButtonText = R.string.app_status_update_recommended_action,
            positiveButtonCallback = { intentUtil.openPlayStore(this) },
            negativeButtonText = R.string.app_status_update_recommended_dismiss_action
        )
    }

    private fun setProductionFlags() {
        if (BuildConfig.FLAVOR == "prod") {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
    }

    override fun onPause() {
        disconnectFromExternalBarcode()
        unregisterReceiver(broadcastReceiver)
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        loading = false
        qrCode = ""
        registerReceiver(broadcastReceiver, IntentFilter(INTENT_ACTION_GRANT_USB))
        connectWithExternalBarcode()
    }

    override fun onStart() {
        super.onStart()
        // Only get app config on every app foreground when introduction is finished
        if (introductionViewModel.getIntroductionStatus() is IntroductionStatus.IntroductionFinished) {
            appStatusViewModel.refresh(mobileCoreWrapper)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {

        if (event.action == KeyEvent.ACTION_DOWN && !loading) {
            loading = true
            findNavController(
                this,
                R.id.main_nav_host_fragment
            ).navigate(
                R.id.action_scanner,
                VerifierQrScannerFragmentArgs(externalScan = true, loading = true).toBundle()
            )
        }

        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER) {
            sendBarcode()
            return true
        } else if (event.action == KeyEvent.ACTION_DOWN && event.isPrintingKey) {
            val pressedKey = event.unicodeChar.toChar()
            qrCode += pressedKey
        } else if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_SPACE) {
            Timber.e("KEY.spatie!!")
            qrCode += " "
        }
        return super.dispatchKeyEvent(event)
    }

    private fun getCustomProber(): UsbSerialProber {
        val customTable = ProbeTable()
        customTable.addProduct(59473, 8450, CdcAcmSerialDriver::class.java) // e.g. Digispark CDC
        return UsbSerialProber(customTable)
    }

    private fun sendBarcode() {
        loading = false

        qrCode = qrCode.trim()

        findNavController(
            this,
            R.id.main_nav_host_fragment
        ).navigate(
            R.id.action_scanner,
            VerifierQrScannerFragmentArgs(qrCode = qrCode, externalScan = true).toBundle()
        )
        qrCode = ""
        Timber.e("QRCODE --> send barcoe")
    }

    override fun onNewData(data: ByteArray?) {
        Timber.e("startUsbDevice - new data")

        mainLooper?.post { receive(data!!) }
    }

    override fun onRunError(e: Exception?) {
        Timber.e("startUsbDevice - onRunError")

        mainLooper?.post {
            status(false, "Verbinding mislukt: ${e?.message}")
            disconnect()
        }
    }

    private enum class UsbPermission {
        Unknown, Requested, Granted, Denied
    }

    /*
     * Serial + UI
     */
    private fun connect() {
        val usbManager = getSystemService(USB_SERVICE) as UsbManager
        if(deviceId <= 0) {
            val custom = getCustomProber().findAllDrivers(usbManager)
            if (custom.isNotEmpty()) {
                deviceId = custom[0].device.deviceId
                portNum = custom[0].ports[0].portNumber
                baudRate = 115200
                withIoManager = true
            } else {
                Toast.makeText(this, "Geen usb barcode scanner gevonden. Klik op de knop rechtsboven.", Toast.LENGTH_LONG).show()
            }
        }

        var device: UsbDevice? = null
        for (v in usbManager.deviceList.values) {
            if (v.deviceId == deviceId) {
                device = v
            }
        }
        if (device == null) {
            status(false, "Verbinding mislukt: geen device gevonden")
            return
        }
        var driver = UsbSerialProber.getDefaultProber().probeDevice(device)
        if (driver == null) {
            driver = getCustomProber().probeDevice(device)
        }
        if (driver == null) {
            status(false, "Verbinding mislukt: geen driver gevonden voor dit device")
            return
        }
        if (driver.ports.size < portNum) {
            status(false, "Verbinding mislukt: niet genoeg porten voor dit device. Start het opnieuw op.")
            return
        }
        usbSerialPort = driver.ports[portNum]
        val usbConnection = usbManager.openDevice(driver.device)
        if (usbConnection == null && usbPermission == UsbPermission.Unknown && !usbManager.hasPermission(
                driver.device
            )
        ) {
            usbPermission = UsbPermission.Requested
            val usbPermissionIntent = PendingIntent.getBroadcast(
                this,
                0,
                Intent(INTENT_ACTION_GRANT_USB),
                0
            )
            usbManager.requestPermission(driver.device, usbPermissionIntent)
            return
        }
        if (usbConnection == null) {
            if (!usbManager.hasPermission(driver.device)) {
                status(false,"Verbinding mislukt: geen toestemming. Start de app opnieuw op.")
            } else {
                status(false,"Verbinding mislukt: kon niet verbinden. Start de app opnieuw op.")
            }
            return
        }
        try {
            usbSerialPort?.open(usbConnection)
            usbSerialPort?.setParameters(baudRate, 8, 1, UsbSerialPort.PARITY_NONE)
            if (withIoManager) {
                usbIoManager = SerialInputOutputManager(usbSerialPort, this).apply {
                    start()
                }
            }
            status(true, "Verbonden")
            connected = true
        } catch (e: Exception) {
            status(false, "Verbinding mislukt: ${e.message}")
            disconnect()
        }
    }

    private fun disconnect() {
        connected = false
        if (usbIoManager != null) {
            usbIoManager?.listener = null
            usbIoManager?.stop()
        }
        usbIoManager = null
        try {
            usbSerialPort!!.close()
        } catch (ignored: IOException) {
        }
        usbSerialPort = null
    }

    private fun receive(data: ByteArray) {
        if (!loading) {
            loading = true
            qrCode = ""
        }
        if (data.isNotEmpty()) {
            qrCode += String(data, StandardCharsets.UTF_8)

            if (qrCode.isNotEmpty() && qrCode.length % 64 != 0) {
                qrCode = qrCode.trim()
                if(qrCode.first().category == CharCategory.CONTROL) {
                    qrCode = qrCode.substring(1)
                }
                sendBarcode()
            }
        }
    }

    private fun status(connected: Boolean, message: String) {
        Timber.e("Status: $message")
        statusListener?.invoke(connected, message)
    }

    fun hasConnectionWithExternalBarcode(): Boolean {
        return connected
    }

    fun connectWithExternalBarcode() {
        mainLooper?.post { connect() }
    }

    fun disconnectFromExternalBarcode(){
        if (connected) {
            status(false, "Verbinding verbroken")
            disconnect()
        }
    }
}
