import { signInWithGoogle, signOut } from 'react-native-google-acm';
import { View, StyleSheet, Button } from 'react-native';

export default function App() {
  return (
    <View style={styles.container}>
      <Button
        title="Sign In"
        onPress={() =>
          signInWithGoogle({
            nonce: '12313123123',
            serverClientId:
              '882363291751-2a37cchrq9oc1lfj1p419otvahnbhguv.apps.googleusercontent.com',
            autoSelectEnabled: false,
            filterByAuthorizedAccounts: false,
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

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
