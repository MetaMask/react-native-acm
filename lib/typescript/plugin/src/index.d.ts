import { type ConfigPlugin } from '@expo/config-plugins';
type Options = {
    iosUrlScheme?: string;
    /**
     * Set to true to skip Firebase / Google Services setup.
     * Only the iosUrlScheme will be registered.
     * @default false
     */
    noFirebase?: boolean;
};
export declare const withGoogleUrlScheme: ConfigPlugin<{
    iosUrlScheme: string;
}>;
declare const _default: ConfigPlugin<Options>;
export default _default;
//# sourceMappingURL=index.d.ts.map