package com.roadwaffle.anubuntu;

public class Config {
	// TODO
	public static final String DATA_DIR = "/sdcard/anubuntu";
	public static final String STARTUP_SCRIPT = DATA_DIR + "/anubuntu.sh";
	public static final String SETUP_COMPLETE = DATA_DIR + "/.setupcomplete";
	
	public static final String GITHUB_REPO = "https://raw.github.com/nealhardesty/anubuntu";
	public static final String STARTUP_SCRIPT_DOWNLOAD_URL = "https://raw.github.com/nealhardesty/anubuntu/master/anubuntu.sh";
	
	public static final String UBUNTU_IMAGE = DATA_DIR + "/ubuntu.img";
	//public static final String UBUNTU_IMAGE_DOWNLOAD_URL = "https://github.com/nealhardesty/anubuntu/blob/master/images/ubuntu_2048m_raring.img.gz";
	public static final String[] UBUNTU_IMAGE_DOWNLOAD_SPLITS = new String[] {
		"https://github.com/nealhardesty/anubuntu/raw/master/images/ubuntu_2048m_raring.img.gz.0",
		"https://github.com/nealhardesty/anubuntu/raw/master/images/ubuntu_2048m_raring.img.gz.1",
		"https://github.com/nealhardesty/anubuntu/raw/master/images/ubuntu_2048m_raring.img.gz.2",
		"https://github.com/nealhardesty/anubuntu/raw/master/images/ubuntu_2048m_raring.img.gz.3",
		"https://github.com/nealhardesty/anubuntu/raw/master/images/ubuntu_2048m_raring.img.gz.4"
	};
}
