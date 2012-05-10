package se.miun.nfc.lib.listeners;

import android.os.Parcelable;

public interface NfcBluetoothListener {
	
	void nfcBluetoothIncommingMessage(Parcelable message);

}
