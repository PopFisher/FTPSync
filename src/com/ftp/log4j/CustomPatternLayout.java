package com.ftp.log4j;

import org.apache.log4j.PatternLayout;
import org.apache.log4j.helpers.PatternParser;

public class CustomPatternLayout extends PatternLayout {

	public CustomPatternLayout(String pattern) {
		super(pattern);
	}

	public CustomPatternLayout() {
		super();
	}

	@Override
	protected PatternParser createPatternParser(String pattern) {
		return new CustomPatternParser(pattern);
	}
}
