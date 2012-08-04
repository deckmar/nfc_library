package se.miun.nfc.lib;

import se.miun.nfc.lib.listeners.NfcTagListener;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class NfcHelperActivity extends Activity {

	protected NfcHelper mNfcHelper;

	protected void listenForAllTags(Object flag, NfcTagListener listener) {
		mNfcHelper.listenForAllTags(flag, listener);
		mNfcHelper.onCreateHook(getIntent());
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mNfcHelper = new NfcHelper(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		mNfcHelper.onPauseHook();
	}

	@Override
	public void onResume() {
		super.onResume();
		mNfcHelper.onResumeHook();
	}

	@Override
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		mNfcHelper.onNewIntentHook(intent);
	}
}
