package com.ftp;


public class Log {
	public void debug(String textString) {
		System.out.println(System.currentTimeMillis() + " " + textString);
	}
	
	public void debug(String textString, Exception e) {
		System.out.println(System.currentTimeMillis() + " " + textString + " exception: " + e);
	}
}
