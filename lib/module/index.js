"use strict";

import { NativeModules, Platform } from 'react-native';
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
  return GoogleAcm.signInWithGoogle(params);
}
export function signOut() {
  return GoogleAcm.signOut();
}
//# sourceMappingURL=index.js.map