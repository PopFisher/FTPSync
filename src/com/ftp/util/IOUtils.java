package com.ftp.util;

import java.io.Closeable;

public class IOUtils {
	public static void silenceClose(Closeable obj) {
		try {
			if (obj != null) {
				obj.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void silenceClose(AutoCloseable obj) {
		try {
			if (obj != null) {
				obj.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
