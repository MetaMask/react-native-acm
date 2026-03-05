package com.googleacm

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ActivityEventListener

import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.Arguments

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

import android.content.Intent
import android.os.Build
import android.util.Log
import android.app.Activity
import androidx.credentials.CredentialManager

import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest

import androidx.credentials.exceptions.GetCredentialException

import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine


class GoogleAcmModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext), ActivityEventListener {

  private val coroutineScope = CoroutineScope(Dispatchers.IO)
  private val LEGACY_SIGN_IN_REQUEST_CODE = 9001
  @Volatile
  private var legacySignInDeferred: CompletableDeferred<ReadableMap?>? = null
  private val legacySignInLock = Any()
  @Volatile
  private var lastLegacyIdToken: String? = null

  init {
    reactContext.addActivityEventListener(this)
  }

  override fun getName(): String {
    return NAME
  }

  @ReactMethod
  fun signInWithGoogle(
    requestObject: ReadableMap,
    promise: Promise
  ) {
    val activity: Activity? = currentActivity
    if (activity == null) {
      promise.reject("E_NO_ACTIVITY", "Current activity is null, cannot launch UI.")
      return
    }

    val credentialManager = CredentialManager.create(activity)

    val nonce = requestObject.getString("nonce") ?: ""
    val serverClientId = requestObject.getString("serverClientId") ?: ""
    val autoSelectEnabled = requestObject.getBoolean("autoSelectEnabled") ?: false
    val filterByAuthorizedAccounts = requestObject.getBoolean("filterByAuthorizedAccounts") ?: false

    val googleIdOption = GetGoogleIdOption
      .Builder()
      .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
      .setServerClientId(serverClientId)
      .setAutoSelectEnabled(autoSelectEnabled)
      .setNonce(nonce)
      .setRequestVerifiedPhoneNumber(false)
      .build()

    coroutineScope.launch {
      try {
        val request: GetCredentialRequest =
          GetCredentialRequest
            .Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result =
          credentialManager.getCredential(
            request = request,
            context = activity,
          )

        val data = handleSignInResult(result)
        if (data != null) {
          promise.resolve(data)
        } else {
          promise.reject("ERROR", "Failed to parse credential response")
        }
      } catch (e: GetCredentialException) {
        Log.w("GoogleAcm", "Credential Manager failed, falling back to legacy sign-in", e)
        try {
          val data = tryLegacySignIn(serverClientId)
          if (data != null) {
            promise.resolve(data)
          } else {
            promise.reject("ERROR", "Legacy sign-in returned no credential")
          }
        } catch (legacyError: Exception) {
          Log.e("GoogleAcm", "Legacy sign-in also failed", legacyError)
          promise.reject(
            "ERROR",
            "Credential Manager failed: ${e.message}; Legacy fallback also failed: ${legacyError.message}"
          )
        }
      } catch (e: Exception) {
        Log.e("GoogleAcm", "Unexpected error during sign-in", e)
        try {
          val data = tryLegacySignIn(serverClientId)
          if (data != null) {
            promise.resolve(data)
          } else {
            promise.reject("ERROR", "Sign-in failed and legacy fallback returned no credential")
          }
        } catch (legacyError: Exception) {
          Log.e("GoogleAcm", "Legacy sign-in also failed", legacyError)
          promise.reject("ERROR", "Sign-in failed: ${e.message}; Legacy fallback also failed: ${legacyError.message}")
        }
      }
    }
  }

  private suspend fun tryLegacySignIn(serverClientId: String): ReadableMap? {
    val activity = currentActivity
      ?: throw Exception("No activity available for legacy sign-in")

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
      .requestIdToken(serverClientId)
      .requestEmail()
      .build()

    val client = GoogleSignIn.getClient(activity, gso)

    try {
      lastLegacyIdToken?.let {
        GoogleAuthUtil.clearToken(reactApplicationContext, it)
        lastLegacyIdToken = null
      }
    } catch (_: Exception) { }

    try {
      suspendCancellableCoroutine<Unit> { cont ->
        client.revokeAccess().addOnCompleteListener { cont.resume(Unit) }
      }
    } catch (_: Exception) { }

    try {
      suspendCancellableCoroutine<Unit> { cont ->
        client.signOut().addOnCompleteListener { cont.resume(Unit) }
      }
    } catch (_: Exception) { }

    delay(300)

    val deferred = CompletableDeferred<ReadableMap?>()
    synchronized(legacySignInLock) {
      legacySignInDeferred?.complete(null)
      legacySignInDeferred = deferred
    }

    withContext(Dispatchers.Main) {
      val signInIntent = client.signInIntent
      activity.startActivityForResult(signInIntent, LEGACY_SIGN_IN_REQUEST_CODE)
    }

    return deferred.await()
  }

  override fun onActivityResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode != LEGACY_SIGN_IN_REQUEST_CODE) return

    val result = handleLegacySignInResult(resultCode, data)
    synchronized(legacySignInLock) {
      legacySignInDeferred?.complete(result)
      legacySignInDeferred = null
    }
  }

  override fun onNewIntent(intent: Intent?) { }

  private fun handleLegacySignInResult(resultCode: Int, data: Intent?): ReadableMap? {
    if (resultCode != Activity.RESULT_OK) {
      Log.w("GoogleAcm", "Legacy sign-in cancelled or failed, resultCode=$resultCode")
      return null
    }

    return try {
      val task = GoogleSignIn.getSignedInAccountFromIntent(data)
      val account = task.getResult(ApiException::class.java)
      val idToken = account?.idToken

      if (idToken != null) {
        lastLegacyIdToken = idToken
        Arguments.createMap().apply {
          putString("type", "google-signin")
          putString("id", account.id ?: "")
          putString("idToken", idToken)
          account.displayName?.let { putString("displayName", it) }
          account.familyName?.let { putString("familyName", it) }
          account.givenName?.let { putString("givenName", it) }
          account.photoUrl?.let { putString("profilePicture", it.toString()) }
        }
      } else {
        Log.e("GoogleAcm", "Legacy sign-in returned null idToken")
        null
      }
    } catch (e: ApiException) {
      Log.e("GoogleAcm", "Legacy sign-in ApiException: statusCode=${e.statusCode}", e)
      null
    }
  }

  fun handleSignInResult(result: androidx.credentials.GetCredentialResponse): ReadableMap? {
    val credential = result.credential
    Log.d("GoogleAcm", "Handle results called")

    return when (credential) {
      is CustomCredential -> {
        if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          try {
            val googleIdTokenCredential =
              GoogleIdTokenCredential
                .createFrom(credential.data)
            Log.d("GoogleAcm", "Google ID Token Credential ID: ${googleIdTokenCredential.id}")

            return Arguments.createMap().apply {
              putString("type", "google-signin")
              putString("id", googleIdTokenCredential.id)
              putString("idToken", googleIdTokenCredential.idToken)
              googleIdTokenCredential.displayName?.let { putString("displayName", it) }
              googleIdTokenCredential.familyName?.let { putString("familyName", it) }
              googleIdTokenCredential.givenName?.let { putString("givenName", it) }
              googleIdTokenCredential.profilePictureUri?.let { putString("profilePicture", it.toString()) }
              googleIdTokenCredential.phoneNumber?.let { putString("phoneNumber", it) }
            }
          } catch (e: GoogleIdTokenParsingException) {
            Log.e("GoogleAcm", "Received an invalid google id token response", e)
            return null
          }
        } else {
          Log.e("GoogleAcm", "Received an unexpected credential type")
          return null
        }
      }

      else -> {
        Log.e("GoogleAcm", "Unexpected type of credential")
        return null
      }
    }
  }

  @ReactMethod
  fun signOut(
    promise: Promise
  ) {
    coroutineScope.launch {
      try {
        handleSignOut()
        promise.resolve(null)
      } catch (e: Exception) {
        Log.e("GoogleAcm", "Error during sign out", e)
        promise.reject("ERROR", "Sign out failed: ${e.message}")
      }
    }
  }

  suspend fun handleSignOut() {
    val activity: Activity? = currentActivity
    if (activity == null) {
      throw Exception("Current activity is null, cannot sign out.")
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      try {
        val credentialManager = CredentialManager.create(activity)
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
      } catch (e: Throwable) {
        Log.w("GoogleAcm", "clearCredentialState failed, will clear legacy sign-in state", e)
      }
    } else {
      Log.d("GoogleAcm", "Skipping clearCredentialState on API ${Build.VERSION.SDK_INT}, using legacy sign-out only")
    }

    try {
      lastLegacyIdToken?.let {
        GoogleAuthUtil.clearToken(reactApplicationContext, it)
      }
    } catch (e: Exception) {
      Log.w("GoogleAcm", "Failed to clear cached token", e)
    }
    lastLegacyIdToken = null

    try {
      val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
      val client = GoogleSignIn.getClient(activity, gso)
      suspendCancellableCoroutine<Unit> { cont ->
        client.signOut().addOnCompleteListener { cont.resume(Unit) }
      }
    } catch (e: Exception) {
      Log.w("GoogleAcm", "Failed to clear legacy sign-in state", e)
    }
  }

  companion object {
    const val NAME = "GoogleAcm"
  }
}
