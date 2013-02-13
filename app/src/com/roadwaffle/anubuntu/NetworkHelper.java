package com.roadwaffle.anubuntu;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import android.os.AsyncTask;
import android.util.Log;

public class NetworkHelper {
	
	// http://www.twmacinta.com/myjava/fast_md5.php
	
	
	public static class FetchSplitToFileTask extends AsyncTask<String, Long, Long> {
		private boolean isGzipped=false;
		private String outputFilename=null;
		public FetchSplitToFileTask(String outputFilename, boolean isGzipped) {
			this.isGzipped=true;
			this.outputFilename=outputFilename;
		}
		@Override
		protected Long doInBackground(String... urls) {
			try {
				//InputStream in = null;
				List<InputStream> streams = new LinkedList<InputStream>();
				InputStream in = null;
				FileOutputStream fout = null;
				try {
					for(String url: urls) {
						URL u = new URL(url);
						URLConnection ucon = u.openConnection();
						streams.add(ucon.getInputStream());
					}
					in = new SequenceInputStream(Collections.enumeration(streams));
					//in = new BufferedInputStream(in);
					if(isGzipped) {
						in = new GZIPInputStream(in);
					}
					fout = new FileOutputStream(outputFilename);

					long contentRead = 0;

					byte data[] = new byte[1024];
					int count;
					while ((count = in.read(data, 0, 1024)) != -1) {
						fout.write(data, 0, count);
						contentRead += count;
						if(contentRead % (1024*10) == 0) {
							publishProgress(contentRead);
						}
					}
					
					return contentRead;
				} finally {
					if (in != null)
						in.close();
					if (fout != null)
						fout.close();
				}
			} catch (Exception ex) {
				Log.e("NetworkHelper.fetch " + outputFilename, "download failed",
						ex);
				return -1l;
			} 
		}
		
	}
	

	/**
	 * Use this only for short downloads (no progress meter)
	 * @param filename
	 * @param url
	 * @param isGzipped
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public static Thread fetchToFile(final String filename, final String url, final boolean isGzipped)
			throws MalformedURLException, IOException {
		final Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					InputStream in = null;
					FileOutputStream fout = null;
					try {
						URL u = new URL(url);
						URLConnection ucon = u.openConnection();
						in = ucon.getInputStream();
						if(isGzipped) {
							in = new GZIPInputStream(in);
						}
						fout = new FileOutputStream(filename);

						byte data[] = new byte[1024];
						int count;
						while ((count = in.read(data, 0, 1024)) != -1) {
							fout.write(data, 0, count);
						}
					} finally {
						if (in != null)
							in.close();
						if (fout != null)
							fout.close();
					}
				} catch (Exception ex) {
					Log.e("NetworkHelper.fetch " + filename, "download failed",
							ex);
				} finally {
					synchronized(Thread.currentThread()) {
						Thread.currentThread().notify();
					}
				}
			}
		});
		t.start();
		return t;
	}

}
