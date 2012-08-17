package se.miun.nfc.lib.listeners;


public interface NfcBluetoothListener {
	
	void nfcBluetoothConnected();
	void nfcBluetoothIncommingMessage(String message);

}
