declare enum ACM_ERROR_CODE {
    NOT_SUPPORTED_ON_IOS = 1
}
export declare class ACM_ERROR extends Error {
    code: ACM_ERROR_CODE;
    constructor(code: ACM_ERROR_CODE, message?: string);
}
export type GoogleSignInParams = {
    nonce?: string;
    serverClientId: string;
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
export declare function signInWithGoogle(params: GoogleSignInParams): Promise<GoogleCredential>;
export declare function signOut(): Promise<void>;
export {};
//# sourceMappingURL=index.d.ts.map