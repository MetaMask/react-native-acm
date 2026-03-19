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
/**
 * Configures App Check for Google Sign-In (iOS only).
 * Call this once as early as possible in your app lifecycle.
 *
 * - Without `appCheckDebugApiKey`: uses production App Attest for real devices.
 * - With `appCheckDebugApiKey`: uses the debug provider for simulators/testing.
 *
 * On Android this is a no-op that resolves immediately.
 */
export function configure(params) {
  if (Platform.OS !== 'ios') {
    return Promise.resolve();
  }
  return GoogleAcm.configure(params ?? {});
}
export function signInWithGoogle(params) {
  return GoogleAcm.signInWithGoogle(params);
}
export function signOut() {
  return GoogleAcm.signOut();
}
//# sourceMappingURL=index.js.map