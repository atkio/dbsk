package name.atkio.dev.android.dosbox;

import name.atkio.dev.android.dosbox.R.id;

import com.fishstix.dosbox.DosBoxLauncher;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.Menu;
import android.widget.TextView;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!CPU.IsKrait()) {
			AlertDialog.Builder dialog = (new AlertDialog.Builder(this));
			dialog.setTitle("NOT supported!");
			dialog.setMessage("Your device is not supported! ");
			dialog.setNeutralButton("Try dosbox turbo",
					new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							String url = "https://play.google.com/store/apps/details?id=com.fishstix.dosbox";
							Intent i = new Intent(Intent.ACTION_VIEW);
							Uri u = Uri.parse(url);
							i.setData(u);
							try {
								startActivity(i);
							} catch (Exception e) {

							}
							MainActivity.this.finish();
						}
					});
			dialog.setOnCancelListener(new AlertDialog.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					MainActivity.this.finish();
				}
			});
			dialog.show();

		} else {
			Thread thread = new Thread(new Runnable() {

				public void run() {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
					}
					Intent mainIntent = new Intent(MainActivity.this,
							DosBoxLauncher.class);
					MainActivity.this.startActivity(mainIntent);
					MainActivity.this.finish();

				}
			});
			thread.start();
		}
		setContentView(R.layout.activity_main);
		TextView textView = (TextView) findViewById(id.maintext);
		textView.setText(CPU.getInfo());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

}
