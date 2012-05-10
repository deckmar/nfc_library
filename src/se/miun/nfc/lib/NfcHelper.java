package se.miun.nfc.lib;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;

import se.miun.nfc.lib.listeners.NfcTagListener;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Parcelable;

public class NfcHelper {

	private static final String TAG = NfcHelper.class.getSimpleName();

	/*
	 * States supported by this Nfc library
	 */
	public final int NFCLIB_STATE_DEFAULT = 0;
	public final int NFCLIB_STATE_LISTEN_ALL_TAGS = 1;

	private int nfclib_state = NFCLIB_STATE_DEFAULT;
	
	/**
	 * Bluetooth flags and strings
	 * 
	 * Message types sent from the BluetoothChatService Handler
	 */
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

	/***
	 * Private objects needed for Nfc
	 */
	private NfcTagListener listener;
	private Activity app;
	private NfcAdapter nfcAdapter;
	private PendingIntent pendingIntent;
	private Object callbackFlag;
	
	/**
	 * Private objects needed for Bluetooth
	 */
	private BluetoothMessageService mBTMessageService;

	public NfcHelper(Activity app) {
		if (app == null)
			throw new IllegalArgumentException("Need to send 'this' (your Activity) as first parameter in NfcHelper constructor");
		this.app = app;
		nfcAdapter = NfcAdapter.getDefaultAdapter(app);
		pendingIntent = PendingIntent.getActivity(app, 0,
				new Intent(app, app.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
	}

	public void listenForAllTags(Object callbackFlag, NfcTagListener listener) {
		if (listener == null)
			throw new IllegalArgumentException("Need a listener for NfcHelper constructor");
		this.listener = listener;
		this.callbackFlag = callbackFlag;
		this.nfclib_state = NFCLIB_STATE_LISTEN_ALL_TAGS;
	}
	
	public boolean hasNfcTag(Intent intent) {
		if ((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0)
			return false;
		if (!intent.hasExtra(NfcAdapter.EXTRA_TAG))
			return false;
		
		return true;
	}

	/***
	 * Hooks on Activity events. These need to be called at the corresponsing
	 * calls in apps using this lib.
	 */

	public void onPauseHook() {
		nfcAdapter.disableForegroundDispatch(app);
	}

	public void onResumeHook() {
		if (this.nfclib_state == NFCLIB_STATE_LISTEN_ALL_TAGS) {
			nfcAdapter.enableForegroundDispatch(this.app, this.pendingIntent, null, null);
		}
	}
	
	public void onCreateHook(Intent intent) {
		if (!this.hasNfcTag(intent))
			return;
		
		this.handleIntent(intent);
	}

	public void onNewIntentHook(Intent intent) {
		
		if (!this.hasNfcTag(intent))
			return;
		
		this.handleIntent(intent);
	}
	
	private void handleIntent(Intent intent) {
		Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		String action = intent.getAction();
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
			Ndef ndefTech = Ndef.get(tag);
			NdefMessage ndefMessage = ndefTech.getCachedNdefMessage();
			this.listener.nfcDiscoverNdef(ndefTech, ndefMessage, this.callbackFlag);
		}

		if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
			this.listener.nfcDiscoverTag(tag, this.callbackFlag);
		}
	}

	/**
	 * Nfc writing methods. These require the Tag object so they should be used
	 * directly from the discover-callbacks.
	 */

	@SuppressWarnings("unused")
	public boolean writeNdefMessage(Tag tag, NdefMessage ndefMessage) {

		// NOTE: Not going to format any non-Ndef tags (to be nice).
		NdefFormatable ndefTech = null; // NdefFormatable.get(tag);

		if (ndefTech != null) {
			try {
				ndefTech.connect();
				ndefTech.format(ndefMessage);
			} catch (Exception e) {
				// e.printStackTrace();
				return false;
			}
		} else {
			Ndef ndef = Ndef.get(tag);
			try {
				ndef.connect();
				ndef.writeNdefMessage(ndefMessage);
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}

		return true;
	}

	public boolean writeNdefApkLinkAndData(Tag tag, Uri apkUri, HashMap<String, String> dataKeyValues) {

		byte[] uriField = apkUri.toString().getBytes(Charset.forName("US-ASCII"));
		byte[] payload = new byte[uriField.length + 1];
		payload[0] = 0x00;
		System.arraycopy(uriField, 0, payload, 1, uriField.length);
		NdefRecord rtdUriRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_URI, new byte[0], payload);
		ArrayList<NdefRecord> records = new ArrayList<NdefRecord>();
		records.add(rtdUriRecord);

		for (String key : dataKeyValues.keySet()) {
			String value = dataKeyValues.get(key);
			NdefRecord rec = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0],
					(key + "=" + value).getBytes(Charset.forName("US-ASCII")));
			records.add(rec);
		}

		NdefMessage ndef = new NdefMessage(records.toArray(new NdefRecord[0]));

		return writeNdefMessage(tag, ndef);
	}
	
	
	/**
	 * Bluetooth handover & communication methods
	 */
	public boolean sendMessage(int msgType, String message) {
		byte[] msg = message.getBytes(Charset.forName("UTF-8"));
		return true;
	}

	/**
	 * Hex converter helper
	 */
	private static final byte[] HEX_CHAR_TABLE = { (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4', (byte) '5',
			(byte) '6', (byte) '7', (byte) '8', (byte) '9', (byte) 'A', (byte) 'B', (byte) 'C', (byte) 'D', (byte) 'E',
			(byte) 'F' };

	private static String getHexString(byte[] raw, int len) {
		byte[] hex = new byte[2 * len];
		int index = 0;
		int pos = 0;

		for (byte b : raw) {
			if (pos >= len)
				break;

			pos++;
			int v = b & 0xFF;
			hex[index++] = HEX_CHAR_TABLE[v >>> 4];
			hex[index++] = HEX_CHAR_TABLE[v & 0xF];
		}

		return new String(hex);
	}
}
