package com.roadwaffle.anubuntu;

import java.io.File;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class SetupActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setup);

		Button startButton = (Button) findViewById(R.id.startButton);
		startButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				initialSetup();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_setup, menu);
		return true;
	}

	private void initialSetup() {
		try {
			final SetupActivity that = this;

			// create the base directory
			(new File(Config.DATA_DIR)).mkdirs();

			// remove the current anubuntu.sh
			File startupScriptFile = new File(Config.STARTUP_SCRIPT);
			if (startupScriptFile.exists())
				startupScriptFile.delete();

			// remove the current image
			File imageFile = new File(Config.UBUNTU_IMAGE);
			if (imageFile.exists())
				imageFile.delete();

			// fetch the startup script
			Thread scriptDownloader = NetworkHelper.fetchToFile(
					Config.STARTUP_SCRIPT, Config.STARTUP_SCRIPT_DOWNLOAD_URL,
					false);

			synchronized (scriptDownloader) {
				scriptDownloader.wait();
			}
			log("Download of " + Config.STARTUP_SCRIPT + " complete");

			NetworkHelper.FetchSplitToFileTask imageDownloader = new NetworkHelper.FetchSplitToFileTask(
					Config.UBUNTU_IMAGE, true) {
				@Override
				protected void onPostExecute(Long result) {
					if (result > 0) {
						log("Download of " + Config.UBUNTU_IMAGE + " Complete");
						that.completeSetup();
					} else {
						log("Failed to download " + Config.UBUNTU_IMAGE);
					}
				}

				@Override
				protected void onPreExecute() {
					log("Starting download of " + Config.UBUNTU_IMAGE);
				}

				@Override
				protected void onProgressUpdate(Long... values) {
					log("Downloaded " + values[0] + " bytes of "
							+ Config.UBUNTU_IMAGE);
				}
			};
			imageDownloader.execute(Config.UBUNTU_IMAGE_DOWNLOAD_SPLITS);

		} catch (Exception ex) {
			// TODO: actually do something with exceptions
			log(ex.toString());
		}
	}

	private void completeSetup() {
		// TODO create the finish complete file and send back to the main
		// activity...
	}

	public void log(String message) {
		EditText et = (EditText) findViewById(R.id.logSetupEditText);
		et.append(message);
		et.append("\n");
		Log.i("SetupActivity#log", message);
	}

}
