package uz.azoff.launcher

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : FragmentActivity() {
    val vm: LauncherViewModel by viewModels()
    lateinit var widgetHost: AppWidgetHost
    lateinit var widgetManager: AppWidgetManager
    private var pendingWidgetId = -1

    /** Uy tugmasi bosilganda ochiq oynalarni yopish uchun signal */
    val homePressed = MutableStateFlow(0)

    private val bindWidget = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val id = pendingWidgetId
        if (id == -1) return@registerForActivityResult
        if (res.resultCode == RESULT_OK) configureOrAdd(id) else cancelWidget(id)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        widgetManager = AppWidgetManager.getInstance(this)
        widgetHost = AppWidgetHost(applicationContext, WIDGET_HOST_ID)
        setContent { LauncherRoot(this) }
    }

    override fun onStart() {
        super.onStart()
        try { widgetHost.startListening() } catch (_: Exception) {}
    }

    override fun onStop() {
        super.onStop()
        try { widgetHost.stopListening() } catch (_: Exception) {}
    }

    override fun onResume() {
        super.onResume()
        vm.onResume()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN) homePressed.value = homePressed.value + 1
    }

    // ---------------- Vidjetlar ----------------

    fun addWidget(provider: AppWidgetProviderInfo) {
        val id = widgetHost.allocateAppWidgetId()
        pendingWidgetId = id
        val allowed = try { widgetManager.bindAppWidgetIdIfAllowed(id, provider.provider) } catch (e: Exception) { false }
        if (allowed) {
            configureOrAdd(id)
        } else {
            val i = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
            try { bindWidget.launch(i) } catch (e: Exception) { cancelWidget(id) }
        }
    }

    private fun configureOrAdd(id: Int) {
        val info = widgetManager.getAppWidgetInfo(id)
        if (info?.configure != null) {
            try {
                widgetHost.startAppWidgetConfigureActivityForResult(this, id, 0, REQ_CONFIGURE, null)
                return
            } catch (_: Exception) {
            }
        }
        finishWidget(id)
    }

    private fun finishWidget(id: Int) {
        vm.update { it.copy(widgetIds = it.widgetIds + id) }
        pendingWidgetId = -1
    }

    private fun cancelWidget(id: Int) {
        try { widgetHost.deleteAppWidgetId(id) } catch (_: Exception) {}
        pendingWidgetId = -1
    }

    fun removeWidget(id: Int) {
        try { widgetHost.deleteAppWidgetId(id) } catch (_: Exception) {}
        vm.update { it.copy(widgetIds = it.widgetIds - id) }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CONFIGURE) {
            val id = pendingWidgetId
            if (id == -1) return
            if (resultCode == RESULT_OK) finishWidget(id) else cancelWidget(id)
        }
    }

    // ---------------- Biometrik tekshiruv ----------------

    private val authenticators =
        BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL

    fun biometricAvailable(): Boolean =
        BiometricManager.from(this).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS

    fun biometric(title: String, onSuccess: () -> Unit, onFail: () -> Unit) {
        if (!biometricAvailable()) {
            onFail(); return
        }
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onFail()
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setAllowedAuthenticators(authenticators)
            .build()
        try { prompt.authenticate(info) } catch (e: Exception) { onFail() }
    }

    companion object {
        const val WIDGET_HOST_ID = 2026
        const val REQ_CONFIGURE = 77
    }
}
