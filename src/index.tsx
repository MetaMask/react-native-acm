import { NativeModules, Platform } from 'react-native';

enum ACM_ERROR_CODE {
  NOT_SUPPORTED_ON_IOS = 1,
}

const ACM_ERROR_CODE_MAP = {
  [ACM_ERROR_CODE.NOT_SUPPORTED_ON_IOS]:
    'Google ACM Sign In is not supported on iOS',
};

export class ACM_ERROR extends Error {
  code: ACM_ERROR_CODE;
  constructor(code: ACM_ERROR_CODE, message?: string) {
    super(message || ACM_ERROR_CODE_MAP[code]);
    this.name = 'ACM_ERROR';
    this.code = code;
  }
}

const LINKING_ERROR =
  `The package '@metamask/react-native-acm' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const GoogleAcm = NativeModules.GoogleAcm
  ? NativeModules.GoogleAcm
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

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

export function signInWithGoogle(
  params: GoogleSignInParams
): Promise<GoogleCredential> {
  if (Platform.OS === 'ios') {
    throw new ACM_ERROR(ACM_ERROR_CODE.NOT_SUPPORTED_ON_IOS);
  }
  return GoogleAcm.signInWithGoogle(params);
}

export function signOut(): Promise<void> {
  if (Platform.OS === 'ios') {
    throw new ACM_ERROR(ACM_ERROR_CODE.NOT_SUPPORTED_ON_IOS);
  }
  return GoogleAcm.signOut();
}
