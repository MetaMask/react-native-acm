"use strict";

import { NativeModules, Platform } from 'react-native';
var ACM_ERROR_CODE = /*#__PURE__*/function (ACM_ERROR_CODE) {
  ACM_ERROR_CODE[ACM_ERROR_CODE["NOT_SUPPORTED_ON_IOS"] = 1] = "NOT_SUPPORTED_ON_IOS";
  return ACM_ERROR_CODE;
}(ACM_ERROR_CODE || {});
const ACM_ERROR_CODE_MAP = {
  [ACM_ERROR_CODE.NOT_SUPPORTED_ON_IOS]: 'Google ACM Sign In is not supported on iOS'
};
export class ACM_ERROR extends Error {
  constructor(code, message) {
    super(message || ACM_ERROR_CODE_MAP[code]);
    this.name = 'ACM_ERROR';
    this.code = code;
  }
}
const LINKING_ERROR = `The package '@metamask/react-native-acm' doesn't seem to be linked. Make sure: \n\n` + Platform.select({
  ios: "- You have run 'pod install'\n",
  default: ''
}) + '- You rebuilt the app after installing the package\n' + '- You are not using Expo Go\n';
const GoogleAcm = NativeModules.GoogleAcm ? NativeModules.GoogleAcm : new Proxy({}, {
  get() {
    throw new Error(LINKING_ERROR);
  }
});
export function signInWithGoogle(params) {
  if (Platform.OS === 'ios') {
    throw new ACM_ERROR(ACM_ERROR_CODE.NOT_SUPPORTED_ON_IOS);
  }
  return GoogleAcm.signInWithGoogle(params);
}
export function signOut() {
  if (Platform.OS === 'ios') {
    throw new ACM_ERROR(ACM_ERROR_CODE.NOT_SUPPORTED_ON_IOS);
  }
  return GoogleAcm.signOut();
}
//# sourceMappingURL=index.js.map