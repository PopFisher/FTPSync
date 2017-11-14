package com.ftp;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.ftp.util.Log;


public class FTPMain {
	
	private static final String LOG_PATH_STRING = "output/";
	
	public static void main(String[] args) {
		try {
			SimpleDateFormat sFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
			String logNameString = sFormat.format(new Date(System.currentTimeMillis()));
			Log.initLogger(LOG_PATH_STRING + logNameString + ".log");
			
			FTPSync.uploadDirectory("D:\\ProgramData\\QQDoc\\774232122\\FileRecv\\lzma", "I:\\testdownload");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
