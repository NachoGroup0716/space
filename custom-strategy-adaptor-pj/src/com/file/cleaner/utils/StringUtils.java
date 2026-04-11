package com.file.cleaner.utils;

import java.util.Objects;

public class StringUtils {
	public static boolean isNullOrEmpty(Object item) {
		return Objects.isNull(item) || String.valueOf(item).trim().isEmpty();
	}
	
	public static boolean isNotNullAndEmpty(Object item) {
		return !isNullOrEmpty(item);
	}
}
