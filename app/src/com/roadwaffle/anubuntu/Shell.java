package com.roadwaffle.anubuntu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Map;

import android.util.Log;

public class Shell {
	protected Process shell = null;
	protected OutputStream stdin = null;
	protected InputStream stdout = null;
	// protected InputStream stderr = null;
	protected BufferedReader reader = null;
	//protected BufferedWriter writer = null;

	protected LineReaderCallback callback = null;
	protected Thread callbackThread = null;
	
	protected short exitCode = -1;

	private byte[] NEWLINE = "\n".getBytes();

	public Shell() throws IOException {
		this(null);
	}
	
	public Shell(Map<String, String> environment) throws IOException {
		this(environment, "sh");
	}

	public Shell(Map<String, String> environment, String... args)
			throws IOException {
		ProcessBuilder pb = new ProcessBuilder(args);
		pb.redirectErrorStream(true);
		initSaneEnvironment(pb.environment());
		if (environment != null) {
			for (String key : environment.keySet()) {
				pb.environment().put(key, environment.get(key));
			}
		}
		shell = pb.start();
		stdin = shell.getOutputStream();
		stdout = shell.getInputStream();
		// stderr = shell.getErrorStream();
		reader = new BufferedReader(new InputStreamReader(stdout));
		//writer = new BufferedWriter(new OutputStreamWriter(stdin));
		setup();
	}


	private void initSaneEnvironment(Map<String, String> environment) {
		environment.put("HOME", "/data");
		environment.put("TERM", "vt100");
		environment.put("LD_LIBRARY_PATH", "/vendor/lib:/system/lib");
		environment.put("HOSTNAME", "android");
		environment.put("PATH",
				"/sbin:/vendor/bin:/system/sbin:/system/bin:/system/xbin");
		environment.put("ANDROID_DATA", "/data");
		environment.put("ANDROID_ROOT", "/system");
		environment.put("SHELL", "/system/bin/sh");
		environment.put("PS1", "");
	}


	static interface LineReaderCallback {
		void handleLine(String line);
	}

	void registerStdoutLineCallbackDefault() {
		registerStdoutLineCallback(new LineReaderCallback() {
			public void handleLine(String line) {
				Log.d("registerStdoutLineCallbackDefault", "SHELL: " + line);
			}
		});
	}

	void registerStdoutLineCallback(LineReaderCallback callback) {
		if (callback == null) {
			return;
		}
		final LineReaderCallback cb = callback;
		callbackThread = new Thread() {
			public void run() {
				try {
					String line = reader.readLine();
					while (null != line
							&& false == Thread.currentThread().isInterrupted() 
							) {
						cb.handleLine(line);
						line = reader.readLine();
					}
				} catch (IOException ioex) {
					Log.w("registerStdoutLineCallback", "registerStdoutLineCallback", ioex);
				}

			}
		};
		callbackThread.start();
	}

	// can be overidden
	protected void setup() {

	}

	// can be overidden() {
	protected void teardown() {
		try {
			send("exit");
		} catch (IOException iex) {
			// ...
		}
	}

	public void send(String line) throws IOException {
		if (line != null) {
			if(!checkShellRunning()) return;
			stdin.write(line.getBytes());
			if(!checkShellRunning()) return;
			stdin.write(NEWLINE);
			if(!checkShellRunning()) return;
			stdin.flush();
		}
	}
	
	private boolean checkShellRunning() {
		boolean isOpen = false;
		int exitCode = -1;
		try {
			exitCode = shell.exitValue();
		} catch (IllegalThreadStateException itse) {
			isOpen = true;
		} 
		if(false == isOpen) {
			this.exitCode = (short)exitCode;
			notifyAll(); // notify anybody calling waitForExit() below
		}
		return isOpen;
	}
	
	/**
	 * Check if we believe the shell is open.  Note that we do not set this until we try and send something to it.
	 */
	public boolean isRunning() {
		return exitCode < 0;
	}
	
	public short waitForExit() {
		try {
			return (short)shell.waitFor();
		} catch (InterruptedException iex) {
			Log.e("Shell", "waitForExit", iex);
		}
		return -1;
	}

	public short getExitCode() {
		return exitCode;
	}

	public void close() throws IOException {
		try {
			teardown();
			if(callbackThread != null) {
				callbackThread.interrupt();
			}
		} finally {
			shell.destroy();
		}
	}
}
