package com.googleacm

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise

import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.Arguments

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import android.util.Log
import android.app.Activity
import androidx.credentials.CredentialManager

import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialOption
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPasswordOption
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential

import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException

import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

import org.json.JSONObject


class GoogleAcmModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  private val coroutineScope = CoroutineScope(Dispatchers.IO)

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

    // Create an instance of CredentialManager with an Activity-based context.
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
        promise.resolve(data)
      } catch (e: GetCredentialException) {
        // ErrorHandler.handleGetCredentialError(e)
        Log.e("CredentialManager", "Error during sign in", e)
        promise.reject("ERROR", "e1 error $e")
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
      } catch (e: GetCredentialException) {

        // ErrorHandler.handleGetCredentialError(e)
        Log.e("CredentialManager", "Error during sign in", e)
        promise.reject("ERROR", "e1 error $e.message.toString()")
      }
    }
  }

  fun handleSignInResult(result: GetCredentialResponse): ReadableMap? {
    // Handle the successfully returned credential.
    val credential = result.credential
    Log.d("CredentialManager", "Handle results called")

    return when (credential) {
      // GoogleIdToken credential
      is CustomCredential -> {
        if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          try {
            val googleIdTokenCredential =
              GoogleIdTokenCredential
                .createFrom(credential.data)
            Log.d("CredentialManager", "Google ID Token Credential ID: ${googleIdTokenCredential.id}")

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
            Log.e("CredentialManager", "Received an invalid google id token response", e)
            return null
          }
        } else {
          Log.e("CredentialManager", "Received an unexpected credential type")
          return null
        }
      }

      else -> {
        // Catch any unrecognized credential type here.
        Log.e("CredentialManager", "Unexpected type of credential")
        return null
      }
    }
  }

  suspend fun handleSignOut() {
    val activity: Activity? = currentActivity
    if (activity == null) {
      throw Exception()
      //promise.reject("E_NO_ACTIVITY", "Current activity is null, cannot launch UI.")
      return
    }

    // Create an instance of CredentialManager with an Activity-based context.
    val credentialManager = CredentialManager.create(activity)

    credentialManager.clearCredentialState(ClearCredentialStateRequest())
  }


  companion object {
    const val NAME = "GoogleAcm"
  }
}
