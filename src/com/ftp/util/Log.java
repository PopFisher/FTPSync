package com.ftp.util;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.ftp.log4j.CustomPatternLayout;

public class Log {
	private static Logger mLogger = Logger.getRootLogger();

	public static void initLogger(String logFileName) {
		try {
			PatternLayout pl = new CustomPatternLayout(
					"%5p %T %d{yyyy/MM/dd HH:mm:ss.SSS} %m%n");
			Logger logger = Logger.getRootLogger();
			logger.removeAllAppenders();
			logger.addAppender(new FileAppender(pl, logFileName, true));
			logger.addAppender(new ConsoleAppender(pl,
					ConsoleAppender.SYSTEM_OUT));
			logger.setLevel(Level.DEBUG);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void trace(String msg) {
		mLogger.trace(msg);
	}

	public static void debug(String msg) {
		mLogger.debug(msg);
	}

	public static void info(String msg) {
		mLogger.info(msg);
	}

	public static void warn(String msg) {
		mLogger.warn(msg);
	}

	public static void error(String msg) {
		mLogger.error(msg);
	}

	public static void error(String msg, Throwable t) {
		mLogger.error(msg, t);
	}
}
