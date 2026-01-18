package com.adaptor.custom.utils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.adaptor.custom.utils.constants.CheckOptions;

public class StringUtils {
	private static final String INDICATOR = "@";
	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile(INDICATOR + "\\{(.*?)\\}");
	
	public static boolean checkAll(Object item, CheckOptions... options) {
		for(CheckOptions option : options) {
			if(!option.check(item)) return false; 
		}
		return true;
	}
	
	public static boolean checkAny(Object item, CheckOptions... options) {
		for(CheckOptions option : options) {
			if(option.check(item)) return true;
		}
		return false;
	}
	
	public static String substitutePlaceholder(String text, Map<String, String> placeholder) {
		if(text == null || placeholder == null || placeholder.isEmpty()) return text;
		
		Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
		StringBuffer sb = new StringBuffer();
		
		while (matcher.find()) {
			String key = matcher.group(1);
			String value = placeholder.get(key);
			if (value == null && placeholder.containsKey(INDICATOR + "{" + key + "}")) {
				value = placeholder.get(INDICATOR + "{" + key + "}");
			}
			String replacement = (value == null) ? "" : Matcher.quoteReplacement(value);
			matcher.appendReplacement(sb, replacement);
		}
		matcher.appendTail(sb);
		
		return sb.toString();
	}
}
