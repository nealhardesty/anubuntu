package com.roadwaffle.anubuntu;

import java.io.IOException;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import com.roadwaffle.anubuntu.Shell.LineReaderCallback;

public class MainActivity extends Activity {

	static Shell shell = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		@SuppressWarnings("unused")
		boolean foo = isRunning();
		wireup();
	}

	private void wireup() {
		Switch enableSwitch = (Switch) findViewById(R.id.enableSwitch);
		enableSwitch.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onEnableSwitchClicked(v);
			}
		});
	}
	private int retCode = 123;
	private boolean isRunning() {
		try { 
			//Shell shell = new Shell(null, "/system/bin/sh", Config.STARTUP_SCRIPT, "issetup");
			//shell.close();
			//int ret = shell.waitForExit();
			//if(ret == 0) {
			//	return true; // environment setup
			//} else {
			//	return false;
			//}
			ProcessBuilder pb = new ProcessBuilder("/system/xbin/su", "/system/bin/sh", Config.STARTUP_SCRIPT, "issetup");
			Process p = pb.start();
			retCode = p.waitFor();
			System.out.println(retCode);
		} catch (IOException ex) {
			Log.e("isRunning", "Start failed", ex);
		} catch (InterruptedException iex) {
			Log.e("isRunning", "interrupted", iex);
		}
 		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	public void onEnableSwitchClicked(View button) {
		if (((Switch) button).isChecked()) {
			// Start the chroot environment
			try {
				startup();
			} catch (IOException ioex) {
				Log.e("onEnableSwitchClicked", "Start failed", ioex);
				makeToast("Start failed");
			}
		} else {
			// Stop the chroot environment
			try {
				teardown();
			} catch (IOException ioex) {
				Log.e("onEnableSwitchClicked", "Stop failed", ioex);
				makeToast("Stop failed");
			}
		}
	}

	private void teardown() throws IOException {
		shell.send(Config.STARTUP_SCRIPT + " stop");
		final MainActivity that = this;
		shell.registerStdoutLineCallback(new LineReaderCallback() {
			public void handleLine(final String line) {
				runOnUiThread(new Runnable() {
					public void run() {
						that.log(line);
					}
				});
				
			}
		});
		shell.close();
	}

	private void startup() throws IOException {
		shell = new SuperuserShell();
		final MainActivity that = this;
		shell.registerStdoutLineCallback(new LineReaderCallback() {
			public void handleLine(final String line) {
				runOnUiThread(new Runnable() {
					public void run() {
						that.log(line);
					}
				});
				
			}
		});
		//shell.registerStdoutLineCallbackDefault();
		shell.send(Config.STARTUP_SCRIPT + " start");

		
		//shell.send(Config.STARTUP_SCRIPT);

	}
	
	public void log(String message) {
		EditText et = (EditText)findViewById(R.id.logEditText);
		et.append(message);
		et.append("\n");
		Log.i("MainActivity#log", message);
	}

	private void makeToast(String message) {
		Context ctx = getApplicationContext();

		Toast toast = Toast.makeText(ctx, message, Toast.LENGTH_SHORT);
		toast.show();
	}
}
