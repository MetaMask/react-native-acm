export type ConfigureParams = {
    /**
     * API key for the App Check debug provider (simulator/testing).
     * When provided, configures the debug provider instead of production App Attest.
     * Create this key in the Google Cloud Console with the Firebase App Check API enabled.
     */
    appCheckDebugApiKey?: string;
};
export type GoogleSignInParams = {
    nonce?: string;
    serverClientId?: string;
    /** iOS-only: OAuth client ID for the iOS app. If omitted, read from GoogleService-Info.plist. */
    iosClientId?: string;
    autoSelectEnabled?: boolean;
    filterByAuthorizedAccounts?: boolean;
};
export type GoogleCredential = {
    type: 'google-signin';
    id: string;
    idToken: string;
    displayName?: string;
    familyName?: string;
    givenName?: string;
    profilePicture?: string;
    phoneNumber?: string;
};
/**
 * Configures App Check for Google Sign-In (iOS only).
 * Call this once as early as possible in your app lifecycle.
 *
 * - Without `appCheckDebugApiKey`: uses production App Attest for real devices.
 * - With `appCheckDebugApiKey`: uses the debug provider for simulators/testing.
 *
 * On Android this is a no-op that resolves immediately.
 */
export declare function configure(params?: ConfigureParams): Promise<void>;
export declare function signInWithGoogle(params: GoogleSignInParams): Promise<GoogleCredential>;
export declare function signOut(): Promise<void>;
//# sourceMappingURL=index.d.ts.map