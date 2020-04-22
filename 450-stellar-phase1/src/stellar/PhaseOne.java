package stellar;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import org.apache.commons.codec.digest.DigestUtils;
import org.stellar.sdk.Asset;
import org.stellar.sdk.AssetTypeCreditAlphaNum;
import org.stellar.sdk.AssetTypeNative;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.Memo;
import org.stellar.sdk.Network;
import org.stellar.sdk.PaymentOperation;
import org.stellar.sdk.Server;
import org.stellar.sdk.Transaction;
import org.stellar.sdk.requests.EventListener;
import org.stellar.sdk.requests.PaymentsRequestBuilder;
import org.stellar.sdk.requests.TooManyRequestsException;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.Page;
import org.stellar.sdk.responses.SubmitTransactionResponse;
import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.PaymentOperationResponse;

public class PhaseOne {

	// Hash Map for accounts <Persons Name, Account Key Pair>
	private static KeyPair keyPair = null;

	// SHA1 hash of my name "Zachary Demaris"
	private static String hashStr = DigestUtils.sha1Hex("Zachary Demaris");

	private static String name = null;

	private static String helpMenu = "Available Commands:\n"
			+ "create_account <name> - Creates an account under the specified name\n"
			+ "get_balance - retrieves the current account balance\n"
			+ "transfer <account_id> Transfers lumens to specified to account_id";

	public static void main(String[] args) throws MalformedURLException, IOException {

		Scanner s = new Scanner(System.in);
		System.out.println("Please enter your name:");
		System.out.print(">");
		name = s.nextLine();
		boolean valid = parseCommand("create_account " + name);
		if (valid) {
			System.out.println("Welcome " + name);
			//setupPaymentHistory();
			while (true) {
				System.out.print(">");
				String command = s.nextLine();
				parseCommand(command);
			}
		}
		
		s.close();

	}

	private static void createAccount(String name) throws MalformedURLException, IOException {

		keyPair = KeyPair.random();

		String friendbotUrl = String.format("https://friendbot.stellar.org/?addr=%s", keyPair.getAccountId());
		InputStream response = new URL(friendbotUrl).openStream();
		String body = new Scanner(response, "UTF-8").useDelimiter("\\A").next();
		System.out.println("SUCCESS! You have a new account!\n");

		Server server = new Server("https://horizon-testnet.stellar.org");
		AccountResponse account = server.accounts().account(keyPair);
		System.out.println("Account ID: " + keyPair.getAccountId());
		System.out.println("Balance: " + getBalance());

	}

	private static Double getBalance() throws IOException {
		if (keyPair != null) {
			Server server = new Server("https://horizon-testnet.stellar.org");
			AccountResponse account = server.accounts().account(keyPair);
			for (AccountResponse.Balance balance : account.getBalances()) {
				return Double.parseDouble(balance.getBalance());
			}
		}
		return 99999999.00;
	}

	private static boolean parseCommand(String command) throws MalformedURLException, IOException {
		String[] commandArr = command.split(" ");
		if (commandArr.length == 2 && commandArr[0].equals("create_account")) {
			createAccount(commandArr[1]);
			return true;
		} else if (commandArr.length == 2 && commandArr[0].equals("transfer")) {
			transfer(commandArr[1]);
			return true;
		} else if (commandArr.length == 1 && commandArr[0].equals("get_balance")) {
			System.out.println("Balance: " + getBalance());
			return true;
		}
		else if (commandArr.length == 1 && commandArr[0].equals("get_history")) {
			setupPaymentHistory();
			return true;
		}
		else if (commandArr.length == 1 && commandArr[0].equals("help")) {
			System.out.println(helpMenu + "\nEnter 'help' to repeat these options\n");
			return true;
		} else {
			System.out.println("Invalid command, please try again");
			return false;
		}

	}

	private static void transfer(String destinationAccount) throws IOException {
		int amount = Character.getNumericValue(hashStr.charAt(0)) + (Character.getNumericValue(hashStr.charAt(1)) * 10)
				+ ((Character.getNumericValue(hashStr.charAt(2)) % 5) * 100);
		System.out.println(amount);
		
		Network.useTestNetwork();
		Server server = new Server("https://horizon-testnet.stellar.org");

		KeyPair source = KeyPair.fromSecretSeed(keyPair.getSecretSeed());
		KeyPair destination = KeyPair.fromAccountId(destinationAccount);

		// First, check to make sure that the destination account exists.
		// You could skip this, but if the account does not exist, you will be charged
		// the transaction fee when the transaction fails.
		// It will throw HttpResponseException if account does not exist or there was
		// another error.
		server.accounts().account(destination);

		// If there was no error, load up-to-date information on your account.
		AccountResponse sourceAccount = server.accounts().account(source);

		// Start building the transaction.
		Transaction transaction = new Transaction.Builder(sourceAccount)
				.addOperation(
						new PaymentOperation.Builder(destination, new AssetTypeNative(), Integer.toString(amount)).build())
				// A memo allows you to add your own metadata to a transaction. It's
				// optional and does not affect how Stellar treats the transaction.
				.addMemo(Memo.text("Test Transaction")).build();
		// Sign the transaction to prove you are actually the person sending it.
		transaction.sign(source);

		// And finally, send it off to Stellar!
		try {
			SubmitTransactionResponse response = server.submitTransaction(transaction);
			System.out.println("Success!");
			System.out.println(response);
		} catch (Exception e) {
			System.out.println("Something went wrong!");
			System.out.println(e.getMessage());
			// If the result is unknown (no response body, timeout etc.) we simply resubmit
			// already built transaction:
			// SubmitTransactionResponse response = server.submitTransaction(transaction);
		}

	}
	
	private static void setupPaymentHistory() throws TooManyRequestsException, IOException {
		Server server = new Server("https://horizon-testnet.stellar.org");

		// Create an API call to query payments involving the account.
		PaymentsRequestBuilder paymentsRequest = server.payments().forAccount(keyPair);		
		Page<OperationResponse> response = paymentsRequest.execute();
		ArrayList<OperationResponse> responseList = response.getRecords();
		
		for(OperationResponse payment : responseList) {
			if(payment instanceof PaymentOperationResponse) {
				String amount = ((PaymentOperationResponse) payment).getAmount();
				//
				Asset asset = ((PaymentOperationResponse) payment).getAsset();
				String assetName;
				if (asset.equals(new AssetTypeNative())) {
					assetName = "lumens";
				} else {
					StringBuilder assetNameBuilder = new StringBuilder();
					assetNameBuilder.append(((AssetTypeCreditAlphaNum) asset).getCode());
					assetNameBuilder.append(":");
					assetNameBuilder.append(((AssetTypeCreditAlphaNum) asset).getIssuer().getAccountId());
					assetName = assetNameBuilder.toString();
				}
				
				StringBuilder output = new StringBuilder();
				output.append(amount);
				output.append(" ");
				output.append(assetName);
			
				if(((PaymentOperationResponse) payment).getFrom().getAccountId().equals(keyPair.getAccountId()))
				{
					output.append(" to ");
					output.append(((PaymentOperationResponse) payment).getTo().getAccountId());
				}
				else {
					output.append(" from ");
					output.append(((PaymentOperationResponse) payment).getFrom().getAccountId());
				}
				
				System.out.println(output.toString());
			}
		}
	}
	

}
