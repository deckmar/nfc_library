package se.miun.nfc.lib;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

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

public class NfcHelper {

	protected static final String TAG = NfcHelper.class.getSimpleName();

	/***
	 * States supported by this Nfc library
	 */
	public final int NFCLIB_STATE_DEFAULT = 0;
	public final int NFCLIB_STATE_LISTEN_ALL_TAGS = 1;

	protected int nfclib_state = NFCLIB_STATE_DEFAULT;

	/**
	 * Properties and their default settings
	 */
	private String conf_rtc_text_language = "en";

	/***
	 * Private objects needed for Nfc
	 */
	protected NfcTagListener listener;
	protected Activity app;
	protected NfcAdapter nfcAdapter;
	protected PendingIntent pendingIntent;
	protected Object callbackFlag;

	public NfcHelper(Activity app) {
		if (app == null)
			throw new IllegalArgumentException("Need to send 'this' (your Activity) as first parameter in NfcHelper constructor");
		this.app = app;
		nfcAdapter = NfcAdapter.getDefaultAdapter(app);
		if (!deviceHasNfc())
			return;
		pendingIntent = PendingIntent.getActivity(app, 0, new Intent(app, app.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
	}

	public boolean deviceHasNfc() {
		return NfcAdapter.getDefaultAdapter(app) != null;
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
		if (deviceHasNfc()) {
			nfcAdapter.disableForegroundDispatch(app);
		}
	}

	public void onResumeHook() {
		if (deviceHasNfc() && this.nfclib_state == NFCLIB_STATE_LISTEN_ALL_TAGS) {
			nfcAdapter.enableForegroundDispatch(this.app, this.pendingIntent, null, null);
		}
	}

	public void onCreateHook(Intent intent) {
		if (!deviceHasNfc() || !this.hasNfcTag(intent))
			return;

		this.handleIntent(intent);
	}

	public void onNewIntentHook(Intent intent) {
		if (!deviceHasNfc() || !this.hasNfcTag(intent))
			return;

		this.handleIntent(intent);
	}

	protected void handleIntent(Intent intent) {
		if (!deviceHasNfc()) {
			return;
		}
		
		Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		String action = intent.getAction();
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
			Ndef ndefTech = Ndef.get(tag);
			if (ndefTech == null)
				return;
			NdefMessage ndefMessage = ndefTech.getCachedNdefMessage();

			/**
			 * Pass on Ndef message
			 */
			this.handleNdef(ndefTech, ndefMessage);
		}

		if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
			this.listener.nfcDiscoverTag(tag, this.callbackFlag);
		}
	}

	protected void handleNdef(Ndef ndefTech, NdefMessage ndefMessage) {
		this.listener.nfcDiscoverNdef(ndefTech, ndefMessage, this.callbackFlag);
	}

	/**
	 * Nfc writing methods. These require the Tag object so they should be used
	 * directly from the discover-callbacks.
	 */

	@SuppressWarnings("unused")
	public boolean writeNdefMessage(Tag tag, NdefMessage ndefMessage) {
		
		if (!deviceHasNfc()) {
			return false;
		}

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
		if (!deviceHasNfc()) {
			return false;
		}
		
		NdefMessage ndef = makeNdefMessageFromUriAndStringMap(apkUri, dataKeyValues);
		return writeNdefMessage(tag, ndef);
	}

	public NdefRecord makeNdefRecordFromUri(Uri apkUri) {
		byte[] uriField = apkUri.toString().getBytes(Charset.forName("US-ASCII"));
		// byte[] payload = new byte[uriField.length + 1];
		// payload[0] = 0x00;
		// System.arraycopy(uriField, 0, payload, 1, uriField.length);
		// NdefRecord rtdUriRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
		// NdefRecord.RTD_URI, new byte[0], payload);
		NdefRecord rtdUriRecord = new NdefRecord(NdefRecord.TNF_ABSOLUTE_URI, uriField, new byte[0], new byte[0]);
		return rtdUriRecord;
	}

	/***
	 * Creates a (TNF_WELL_KNOWN) RTD_TEXT NdefRecord in UTF-8 format. See:
	 * http://developer.android.com/guide/topics/connectivity/nfc/nfc.html#mime
	 * 
	 * @param text
	 *            The text to encode
	 * @return NdefRecord
	 */
	public NdefRecord makeNdefRecordFromText(String text) {
		byte[] langBytes = conf_rtc_text_language.getBytes(Charset.forName("US-ASCII"));
		byte[] textBytes = text.getBytes(Charset.forName("UTF-8"));
		char status = (char) (langBytes.length);
		byte[] data = new byte[1 + langBytes.length + textBytes.length];
		data[0] = (byte) status;
		System.arraycopy(langBytes, 0, data, 1, langBytes.length);
		System.arraycopy(textBytes, 0, data, 1 + langBytes.length, textBytes.length);
		NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], data);
		return record;
	}

	public String extractTextFromTextRecord(NdefRecord rec) {
		if (rec.getTnf() != NdefRecord.TNF_WELL_KNOWN || !java.util.Arrays.equals(rec.getType(), NdefRecord.RTD_TEXT))
			return null;

		int langLength = (int) rec.getPayload()[0];
		String text = new String(rec.getPayload(), 1 + langLength, rec.getPayload().length - 1 - langLength, Charset.forName("UTF-8"));
		return text;
	}

	public NdefMessage makeNdefMessageFromUriAndStringMap(Uri apkUri, HashMap<String, String> dataKeyValues) {
		NdefRecord rtdUriRecord = makeNdefRecordFromUri(apkUri);
		ArrayList<NdefRecord> records = new ArrayList<NdefRecord>();
		records.add(rtdUriRecord);

		for (String key : dataKeyValues.keySet()) {
			String value = dataKeyValues.get(key);
			NdefRecord rec = makeNdefRecordFromText(key + "=" + value);
			records.add(rec);
		}

		NdefMessage ndef = new NdefMessage(records.toArray(new NdefRecord[0]));
		return ndef;
	}

	/**
	 * Hex converter helper
	 */
	private static final byte[] HEX_CHAR_TABLE = { (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6', (byte) '7', (byte) '8', (byte) '9',
			(byte) 'A', (byte) 'B', (byte) 'C', (byte) 'D', (byte) 'E', (byte) 'F' };

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
