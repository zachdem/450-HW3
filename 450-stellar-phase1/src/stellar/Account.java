package stellar;
import org.stellar.sdk.KeyPair;


public class Account {

	private KeyPair keyPair;

	public Account(KeyPair keyPair) {
		this.keyPair = keyPair;
	}

	public KeyPair getKeyPair() {
		return keyPair;
	}

	public void setKeyPair(KeyPair keyPair) {
		this.keyPair = keyPair;
	}
	
}
