import type { ExpoConfig } from '@expo/config-types';
import {
  type ConfigPlugin,
  AndroidConfig,
  IOSConfig,
  createRunOncePlugin,
  withPlugins,
  withInfoPlist,
} from '@expo/config-plugins';

// eslint-disable-next-line @typescript-eslint/no-var-requires
const pkg = require('../../package.json');

type Options = {
  iosUrlScheme?: string;
  /**
   * Set to true to skip Firebase / Google Services setup.
   * Only the iosUrlScheme will be registered.
   * @default false
   */
  noFirebase?: boolean;
};

function validateUrlScheme(iosUrlScheme: string) {
  const messagePrefix = `@metamask/react-native-acm config plugin`;
  if (!iosUrlScheme.startsWith('com.googleusercontent.apps.')) {
    throw new Error(
      `${messagePrefix}: \`iosUrlScheme\` must start with "com.googleusercontent.apps.": ${JSON.stringify({ iosUrlScheme })}`
    );
  }
}

export const withGoogleUrlScheme: ConfigPlugin<{ iosUrlScheme: string }> = (
  config,
  options
) => {
  return withInfoPlist(config, (cfg) => {
    const scheme = options.iosUrlScheme;
    const infoPlist = cfg.modResults;
    if (!IOSConfig.Scheme.hasScheme(scheme, infoPlist)) {
      cfg.modResults = IOSConfig.Scheme.appendScheme(scheme, infoPlist);
    }
    return cfg;
  });
};

/**
 * Firebase mode: uses Expo's built-in helpers to wire up Google Services
 * on Android (classpath, plugin, google-services.json) and iOS
 * (Google config, GoogleService-Info.plist).
 */
const withGoogleSignIn: ConfigPlugin = (config: ExpoConfig) => {
  return withPlugins(config, [
    // Android
    AndroidConfig.GoogleServices.withClassPath,
    AndroidConfig.GoogleServices.withApplyPlugin,
    AndroidConfig.GoogleServices.withGoogleServicesFile,

    // iOS
    IOSConfig.Google.withGoogle,
    IOSConfig.Google.withGoogleServicesFile,
  ]);
};

const withGoogleSignInRoot: ConfigPlugin<Options | void> = (
  config: ExpoConfig,
  options
) => {
  const iosUrlScheme = options?.iosUrlScheme;
  const noFirebase = options?.noFirebase ?? false;

  if (iosUrlScheme) {
    validateUrlScheme(iosUrlScheme);
  }

  const plugins: ConfigPlugin[] = [];

  if (!noFirebase) {
    plugins.push(withGoogleSignIn);
  }

  if (iosUrlScheme) {
    plugins.push((cfg) => withGoogleUrlScheme(cfg, { iosUrlScheme }));
  }

  return withPlugins(config, plugins);
};

export default createRunOncePlugin<Options>(
  withGoogleSignInRoot,
  pkg.name,
  pkg.version
);
