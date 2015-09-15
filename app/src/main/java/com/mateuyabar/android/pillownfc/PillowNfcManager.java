package com.mateuyabar.android.pillownfc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Parcelable;
import com.mateuyabar.android.pillownfc.NFCWriteException.NFCErrorType;
import java.io.IOException;

//import com.google.common.base.Preconditions;

/**
 * NFC manager.
 * 
 * 
 */
public class PillowNfcManager {
	NfcAdapter nfcAdapter;
	Activity activity;
	PendingIntent pendingIntent;

	TagReadListener onTagReadListener;
	TagWriteListener onTagWriteListener;
	TagWriteErrorListener onTagWriteErrorListener;

	String writeText = null;

	
	public PillowNfcManager(Activity activity) {
		this.activity = activity;
	}

	/**
	 * Sets the listener to read events
	 */
	public void setOnTagReadListener(TagReadListener onTagReadListener) {
		this.onTagReadListener = onTagReadListener;
	}

	/**
	 * Sets the listener to write events
	 */
	public void setOnTagWriteListener(TagWriteListener onTagWriteListener) {
		this.onTagWriteListener = onTagWriteListener;
	}

	/**
	 * Sets the listener to write error events
	 */
	public void setOnTagWriteErrorListener(TagWriteErrorListener onTagWriteErrorListener) {
		this.onTagWriteErrorListener = onTagWriteErrorListener;
	}

	/**
	 * Indicates that we want to write the given text to the next tag detected
	 */
	public void writeText(String writeText) {
		this.writeText = writeText;
	}

	/**
	 * Stops a writeText operation
	 */
	public void undoWriteText() {
		this.writeText = null;
	}

	
	/**
	 * To be executed on OnCreate of the activity
	 * @return true if the device has nfc capabilities
	 */
	public boolean onActivityCreate() {
		nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
		pendingIntent = PendingIntent.getActivity(activity, 0,
				new Intent(activity, activity.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
		return nfcAdapter!=null;
	}

	/**
	 * To be executed on onResume of the activity
	 */
	public void onActivityResume() {
		if (nfcAdapter != null) {
			if (!nfcAdapter.isEnabled()) {
				//TODO indicate that wireless should be opened
			}
			nfcAdapter.enableForegroundDispatch(activity, pendingIntent, null, null);
		}
	}

	/**
	 * To be executed on onPause of the activity
	 */
	public void onActivityPause() {
		if (nfcAdapter != null) {
			nfcAdapter.disableForegroundDispatch(activity);
		}
	}

	/**
	 * To be executed on onNewIntent of activity
	 * @param intent
	 */
	public void onActivityNewIntent(Intent intent) {
		// TODO Check if the following line has any use 
		// activity.setIntent(intent);
		if (writeText == null)
			readTagFromIntent(intent);
		else {
			Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			try {
				writeTag(activity, tag, writeText);
				onTagWriteListener.onTagWritten();
			} catch (NFCWriteException exception) {
				onTagWriteErrorListener.onTagWriteError(exception);
			} finally {
				writeText = null;
			}
		}
	}

	/**
	 * Reads a tag for a given intent and notifies listeners
	 * @param intent
	 */
	private void readTagFromIntent(Intent intent) {
		String action = intent.getAction();
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
			Tag myTag = (Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
			if (rawMsgs != null) {
				NdefRecord[] records = ((NdefMessage) rawMsgs[0]).getRecords();
				String text = ndefRecordToString(records[0]);
				onTagReadListener.onTagRead(text);
			}
		}
	}

	public String ndefRecordToString(NdefRecord record) {
		byte[] payload = record.getPayload();
		return new String(payload);
	}

	/**
	 * Writes a text to a tag
	 * @param context
	 * @param tag
	 * @param data
	 * @throws NFCWriteException
	 */
	protected void writeTag(Context context, Tag tag, String data) throws NFCWriteException {
		// Record with actual data we care about
		NdefRecord relayRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, null, data.getBytes());


		// Complete NDEF message with both records
		NdefMessage message = new NdefMessage(new NdefRecord[] { relayRecord });

		Ndef ndef = Ndef.get(tag);
		if (ndef != null) {
			// If the tag is already formatted, just write the message to it
			try {
				ndef.connect();
			} catch (IOException e) {
				throw new NFCWriteException(NFCWriteException.NFCErrorType.unknownError);
			}
			// Make sure the tag is writable
			if (!ndef.isWritable()) {
				throw new NFCWriteException(NFCErrorType.ReadOnly);
			}

			// Check if there's enough space on the tag for the message
			int size = message.toByteArray().length;
			if (ndef.getMaxSize() < size) {
				throw new NFCWriteException(NFCErrorType.NoEnoughSpace);
			}

			try {
				// Write the data to the tag
				ndef.writeNdefMessage(message);
			} catch (TagLostException tle) {
				throw new NFCWriteException(NFCWriteException.NFCErrorType.tagLost, tle);
			} catch (IOException ioe) {
				throw new NFCWriteException(NFCErrorType.formattingError, ioe);// nfcFormattingErrorTitle
			} catch (FormatException fe) {
				throw new NFCWriteException(NFCErrorType.formattingError, fe);
			}
		} else {
			// If the tag is not formatted, format it with the message
			NdefFormatable format = NdefFormatable.get(tag);
			if (format != null) {
				try {
					format.connect();
					format.format(message);
				} catch (TagLostException tle) {
					throw new NFCWriteException(NFCErrorType.tagLost, tle);
				} catch (IOException ioe) {
					throw new NFCWriteException(NFCErrorType.formattingError, ioe);
				} catch (FormatException fe) {
					throw new NFCWriteException(NFCErrorType.formattingError, fe);
				}
			} else {
				throw new NFCWriteException(NFCErrorType.noNdefError);
			}
		}

	}

	public interface TagReadListener {
		public void onTagRead(String tagRead);
	}

	public interface TagWriteListener {
		public void onTagWritten();
	}

	public interface TagWriteErrorListener {
		public void onTagWriteError(NFCWriteException exception);
	}
}
