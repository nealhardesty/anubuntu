package com.roadwaffle.anubuntu;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
		
		if(isFirstSetup()) {
			log("MainActivity: This appears to be your first run.  Starting Setup Wizard.");
			Intent setupIntent = new Intent(this, SetupActivity.class);
			startActivity(setupIntent);
		}
		log("AnUbuntu started at " + (new Date()).toString());
		wireup();
	}
	
	private boolean isFirstSetup() {
		return (new File(Config.SETUP_COMPLETE).exists() == false);
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
//				shell.send("/system/bin/ls");
//				shell.send("/system/bin/ls /sdcard");
//				shell.send("/system/xbin/echo foo foo foo");
//				assert(shell.isRunning());
//				shell.send("/system/bin/id");
//				shell.send("exit");
//				assert(false == shell.isRunning());
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
		shell.send("exit\n");
		shell.close();
	}

	private void startup() throws IOException {
		shell = new SuperuserShell();
		//shell.registerStdoutLineCallbackDefault();
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
