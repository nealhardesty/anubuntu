package com.roadwaffle.anubuntu;

import java.io.IOException;
import java.util.Map;

public class SuperuserShell extends Shell {
	public SuperuserShell() throws IOException {
		super("su", null);
	}
	public SuperuserShell(Map<String, String> environment)
			throws IOException {
		super("su", environment);
	}
}
