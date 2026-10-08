package com.silifton.textwright

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.silifton.textwright.security.AppLock
import com.silifton.textwright.ui.MainViewModel
import com.silifton.textwright.ui.TextwrightRoot

/** A FragmentActivity because the fingerprint prompt needs one. */
class MainActivity : FragmentActivity() {

    private val vm: MainViewModel by viewModels()
    private var isDefault by mutableStateOf(false)
    private var permissionsRequested = false

    private val roleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { checkDefault() }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { vm.start() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppLock.onLaunch(this)
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            TextwrightRoot(
                vm = vm,
                isDefault = isDefault,
                onRequestDefault = ::requestDefault,
                authenticate = ::authenticate,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        checkDefault()
        // Offer the fingerprint once per lock; after that the lock screen's button brings it back.
        if (AppLock.locked && !AppLock.prompted && AppLock.biometricEnabled(this)) {
            AppLock.prompted = true
            authenticate("Unlock Textwright", AppLock::unlock)
        }
    }

    override fun onStop() {
        super.onStop()
        // Leaving the app locks it; a rotation does not.
        if (!isChangingConfigurations) AppLock.lock(this)
    }

    private fun authenticate(title: String, onSuccess: () -> Unit) {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
        }
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
        BiometricPrompt(this, ContextCompat.getMainExecutor(this), callback).authenticate(info)
    }

    private fun checkDefault() {
        isDefault = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_SMS)
        } else {
            Telephony.Sms.getDefaultSmsPackage(this) == packageName
        }
        if (!isDefault) return
        vm.start()
        requestMissingPermissions()
    }

    private fun requestDefault() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getSystemService(RoleManager::class.java).createRequestRoleIntent(RoleManager.ROLE_SMS)
        } else {
            @Suppress("DEPRECATION")
            Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                .putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
        }
        roleLauncher.launch(intent)
    }

    /** The SMS role grants the SMS permissions itself; contacts, SIM info and notifications still need asking. */
    private fun requestMissingPermissions() {
        if (permissionsRequested) return
        permissionsRequested = true
        val wanted = buildList {
            add(Manifest.permission.READ_SMS)
            add(Manifest.permission.SEND_SMS)
            add(Manifest.permission.RECEIVE_SMS)
            add(Manifest.permission.READ_CONTACTS)
            add(Manifest.permission.READ_PHONE_STATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val missing = wanted.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray())
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        val threadId = intent.getLongExtra(EXTRA_THREAD_ID, -1)
        if (threadId >= 0) {
            vm.openThread(threadId, intent.getStringExtra(EXTRA_ADDRESS).orEmpty())
            return
        }
        if (intent.action == Intent.ACTION_SENDTO || intent.action == Intent.ACTION_SEND) {
            val address = intent.data?.schemeSpecificPart?.substringBefore('?').orEmpty()
            val body = intent.getStringExtra("sms_body") ?: intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            vm.openCompose(address, body)
        }
    }

    companion object {
        const val EXTRA_THREAD_ID = "thread_id"
        const val EXTRA_ADDRESS = "address"
    }
}
