import GoogleSignIn

@objc(GoogleAcm)
class GoogleAcm: NSObject {

  @objc(configure:withResolver:withRejecter:)
  func configure(
    params: NSDictionary,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    let debugApiKey = params["appCheckDebugApiKey"] as? String

    if let apiKey = debugApiKey, !apiKey.isEmpty {
      if #available(iOS 14, *) {
        GIDSignIn.sharedInstance.configureDebugProvider(withAPIKey: apiKey) { error in
          if let error = error {
            reject("E_APP_CHECK", "Failed to configure App Check debug provider: \(error.localizedDescription)", error)
          } else {
            resolve(nil)
          }
        }
      } else {
        reject("E_APP_CHECK", "App Check debug provider requires iOS 14+", nil)
      }
    } else {
      GIDSignIn.sharedInstance.configure { error in
        if let error = error {
          reject("E_APP_CHECK", "Failed to configure App Check: \(error.localizedDescription)", error)
        } else {
          resolve(nil)
        }
      }
    }
  }

  @objc(signInWithGoogle:withResolver:withRejecter:)
  func signInWithGoogle(
    params: NSDictionary,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    let serverClientId = params["serverClientId"] as? String
    let nonce = params["nonce"] as? String
    let iosClientId = params["iosClientId"] as? String
    let iosUrlScheme = params["iosUrlScheme"] as? String
    let hostedDomain = params["hostedDomain"] as? String
    let openIDRealm = params["openIDRealm"] as? String


    guard let clientId = iosClientId ??  resolveClientId() else {
      reject(
        "E_CONFIG",
        "Failed to determine client ID. Pass iosClientId, iosUrlScheme, or add GoogleService-Info.plist with a CLIENT_ID entry to your app bundle.",
        nil
      )
      return
    }

    let config = GIDConfiguration(
      clientID: clientId,
      serverClientID: serverClientId,
      hostedDomain: hostedDomain,
      openIDRealm: openIDRealm
    )
    GIDSignIn.sharedInstance.configuration = config

    DispatchQueue.main.async {
      guard let presentingVC = RCTPresentedViewController() else {
        reject("E_NO_PRESENTER", "No presenting view controller found.", nil)
        return
      }

      if let nonce = nonce, !nonce.isEmpty {
        GIDSignIn.sharedInstance.signIn(
          withPresenting: presentingVC,
          hint: nil,
          additionalScopes: nil,
          nonce: nonce
        ) { result, error in
          self.handleResult(result, error: error, resolve: resolve, reject: reject)
        }
      } else {
        GIDSignIn.sharedInstance.signIn(withPresenting: presentingVC) { result, error in
          self.handleResult(result, error: error, resolve: resolve, reject: reject)
        }
      }
    }
  }

  @objc(signOut:withRejecter:)
  func signOut(
    resolve: RCTPromiseResolveBlock,
    reject: RCTPromiseRejectBlock
  ) {
    GIDSignIn.sharedInstance.signOut()
    resolve(nil)
  }

  // MARK: - Private

  private func resolveClientId() -> String? {
    guard let path = Bundle.main.url(forResource: "GoogleService-Info", withExtension: "plist"),
          let plist = NSDictionary(contentsOf: path),
          let clientId = plist["CLIENT_ID"] as? String else {
      return nil
    }
    return clientId
  }

  private func handleResult(
    _ result: GIDSignInResult?,
    error: Error?,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    if let error = error as NSError? {
      if error.code == GIDSignInError.canceled.rawValue {
        reject("ERROR", "User cancelled", error)
        return
      }
      reject("ERROR", "Sign-in failed: \(error.localizedDescription)", error)
      return
    }

    guard let user = result?.user,
          let idToken = user.idToken?.tokenString else {
      reject("ERROR", "Sign-in returned no user or id token", nil)
      return
    }

    var response: [String: Any] = [
      "type": "google-signin",
      "id": user.userID ?? "",
      "idToken": idToken,
    ]

    if let name = user.profile?.name {
      response["displayName"] = name
    }
    if let givenName = user.profile?.givenName {
      response["givenName"] = givenName
    }
    if let familyName = user.profile?.familyName {
      response["familyName"] = familyName
    }
    if user.profile?.hasImage == true,
       let imageURL = user.profile?.imageURL(withDimension: 120) {
      response["profilePicture"] = imageURL.absoluteString
    }

    resolve(response)
  }
}
