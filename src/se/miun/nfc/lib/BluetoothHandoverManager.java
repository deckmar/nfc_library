package se.miun.nfc.lib;

import java.util.UUID;

import se.miun.nfc.lib.listeners.NfcBluetoothListener;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BluetoothHandoverManager {
	
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
	
	/**
	 * Private objects needed for Bluetooth
	 * 
	 */
	private BluetoothMessageService mBTMessageService;
	private NfcBluetoothListener mBTListener;
	private BluetoothAdapter mBTAdapter;
	private NfcAdapter mNfcAdapter;
	
	private boolean D = true;
	private String TAG = BluetoothHandoverManager.class.getSimpleName();
	
	public BluetoothHandoverManager(NfcBluetoothListener listener, NfcAdapter mNfcAdapter, UUID app_uuid) {
		mBTListener = listener;
		mBTAdapter = BluetoothAdapter.getDefaultAdapter();
		mBTMessageService = new BluetoothMessageService(mBTEventHandler, app_uuid);
		
		Log.d(TAG, "I'm " + getBluetoothHardwareAddress());
	}
	
	public void activateBluetooth() {
		mBTAdapter.enable();
	}
	
	public String getBluetoothHardwareAddress() {
		return mBTAdapter.getAddress();
	}
	
	public BluetoothAdapter getBTAdapter() {
		return this.mBTAdapter;
	}
	
	public BluetoothDevice getBluetoothDeviceFromAddress(String address) {
		return mBTAdapter.getRemoteDevice(address);
	}
	
	public void listenForConnection() {
		mBTMessageService.start();
	}
	
	public void connect(BluetoothDevice device) {
		mBTMessageService.connect(device, false);
	}
	
	public void write(byte[] message) {
		mBTMessageService.write(message);
	}
	
	private Handler mBTEventHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothMessageService.STATE_CONNECTED:
//                    mTitle.setText(R.string.title_connected_to);
//                    mTitle.append(mConnectedDeviceName);
//                    mConversationArrayAdapter.clear();
                	mBTListener.nfcBluetoothConnected();
                    break;
                case BluetoothMessageService.STATE_CONNECTING:
//                    mTitle.setText(R.string.title_connecting);
                    break;
                case BluetoothMessageService.STATE_LISTEN:
                case BluetoothMessageService.STATE_NONE:
//                    mTitle.setText(R.string.title_not_connected);
                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
//                mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                mBTListener.nfcBluetoothIncommingMessage(readMessage);
//                mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
//                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
//                Toast.makeText(getApplicationContext(), "Connected to "
//                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
//                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
//                               Toast.LENGTH_SHORT).show();
                break;
            }
		}
	};

}
