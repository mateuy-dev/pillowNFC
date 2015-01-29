package cat.my.android.pillownfc.util;

import cat.my.android.nfcpillow.R;
import cat.my.android.pillownfc.NFCWriteException;
import cat.my.android.pillownfc.PillowNfcManager;
import cat.my.android.pillownfc.PillowNfcManager.TagWriteErrorListener;
import cat.my.android.pillownfc.PillowNfcManager.TagWriteListener;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.Toast;

/**
 * Helper for writing tags. Creates a dialog while waiting for the tag, and displays error messages with a toast
 */
public class WriteTagHelper implements TagWriteErrorListener, TagWriteListener{
	AlertDialog dialog;
	PillowNfcManager nfcManager;
	Context context;
	public WriteTagHelper(Context context, PillowNfcManager nfcManager) {
		this.context = context;
		this.nfcManager = nfcManager;
	}
	
	public void writeText(String text){
		dialog = createWaitingDialog();
		dialog.show();
		nfcManager.writeText(text);
	}
	

	@Override
	public void onTagWritten() {
		dialog.dismiss();
		Toast.makeText(context, R.string.tag_written_toast, Toast.LENGTH_LONG).show();;
	}

	@Override
	public void onTagWriteError(NFCWriteException exception) {
		dialog.dismiss();
		//TODO translate exeptions
		Toast.makeText(context, exception.getType().toString(), Toast.LENGTH_LONG).show();
	}
	
	/**
	 * Creates a dialog while waiting for the tag
	 * @return
	 */
	public AlertDialog createWaitingDialog(){
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.wait_write_dialog_title)
		.setMessage(R.string.wait_write_dialog_text)
       .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
           public void onClick(DialogInterface dialog, int id) {
        	   nfcManager.undoWriteText();
           }
       });
		return builder.create();
	}
	
	

	

}
