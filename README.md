# @metamask/react-native-acm

Google ACM for react-native

## Installation

Coming soon.

## Usage

```js
import { signInWithGoogle } from '@metamask/react-native-acm';

// ...
export default function App() {
  return (
    <View style={styles.container}>
      <Button
        title="Sign In With Google"
        onPress={() =>
          signInWithGoogle({
            nonce: '12313123123',
            serverClientId:
              '882363291751-2a37cchrq9oc1lfj1p419otvahnbhguv.apps.googleusercontent.com',
            autoSelectEnabled: false,
            filterByAuthorizedAccounts: true,
          })
            .then((credential) => {
              console.log(credential);
            })
            .catch((error) => {
              console.log(error);
            })
        }
      />
      <Button title="Sign Out" onPress={() => signOut()} />
    </View>
  );
}
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
