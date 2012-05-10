package se.miun.nfc.lib.listeners;

import android.nfc.NdefMessage;
import android.nfc.Tag;
import android.nfc.tech.Ndef;

public interface NfcTagListener {

	void nfcDiscoverTag(Tag tag, Object callbackFlag);

	void nfcDiscoverNdef(Ndef tag, NdefMessage ndefMessage, Object callbackFlag);

}