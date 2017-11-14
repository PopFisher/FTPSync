package com.ftp;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class FTPConfig {

	private static String mServerAddress 	= "10.20.228.50";
	private static int mServerPort 			= 21;
	private static String mUserName 		= "test";
	private static String mPassword 		= "test";
	private static String mRootPath 		= "/v2";
	private static String mSystem 			= "WINDOWS";
	
	private static final String KEY_SERVER 		= "server";
	private static final String KEY_PORT 		= "port";
	private static final String KEY_USERNAME 	= "username";
	private static final String KEY_PASSWORD 	= "password";
	private static final String KEY_ROOTPATH 	= "rootpath";
	private static final String KEY_SYSTEM 		= "system";

	static {
		 parseFtpConfig("cfg/ftp.ini");
	}

	public static String getServerAddress() {
		return mServerAddress;
	}

	public static int getServerPort() {
		return mServerPort;
	}

	public static String getUserName() {
		return mUserName;
	}

	public static String getPassword() {
		return mPassword;
	}

	public static String getRootPath() {
		return mRootPath;
	}

	public static String getSystem() {
		return mSystem;
	}

	private static void parseFtpConfig(String fileName) {
		BufferedReader reader = null;
		String tmp = null;
		try {
			reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(fileName), "UTF-8"));
			while ((tmp = reader.readLine()) != null) {

				if ("".equals(tmp) || tmp.charAt(0) == '#') {
					continue;
				}

				String[] array = tmp.split("=");
				if (array.length != 2) {
					continue;
				}

				String key = array[0];
				String value = array[1];

				if (key.equals(KEY_SERVER)) {
					mServerAddress = value;
				} else if (key.equals(KEY_PORT)) {
					mServerPort = Integer.parseInt(value);
				} else if (key.equals(KEY_USERNAME)) {
					mUserName = value;
				} else if (key.equals(KEY_PASSWORD)) {
					mPassword = value;
				} else if (key.equals(KEY_ROOTPATH)) {
					mRootPath = value;
				} else if (key.equals(KEY_SYSTEM)) {
					mSystem = value;
				}
			}
		} catch (Throwable e) {
			com.ftp.util.Log.error("Exception", e);
		} finally {
			com.ftp.util.IOUtils.silenceClose(reader);
		}
	}
}
