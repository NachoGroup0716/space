package com.file.cleaner.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.file.cleaner.constants.FILE_ATTRIBUTES;
import com.file.cleaner.utils.FileUtils;
import com.file.cleaner.utils.StringUtils;

public class CleanerFileData {
	public static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("@\\{([A-Za-z]+)\\}");
	private final EnumMap<FILE_ATTRIBUTES, Object> attributes = new EnumMap<FILE_ATTRIBUTES, Object>(FILE_ATTRIBUTES.class);
	private final Path path;
	private final BasicFileAttributes attrs;
	private final LocalDateTime now;
	private final LocalDateTime dateTimeBaseOnName;
	
	private LocalDateTime created;
	private LocalDateTime modified;
	
	private String name;
	private boolean isDirectory;
	
	public CleanerFileData(Path path, BasicFileAttributes attrs, LocalDateTime now, String pattern) {
		this.path = path;
		this.attrs = attrs;
		this.now = now;
		this.dateTimeBaseOnName = this.extractDateTimeFromPath(path.toAbsolutePath().toString(), pattern);
		this.name = path.getFileName().toString();
		this.isDirectory = Files.isDirectory(path);
	}
	
	public String toString(boolean result) {
		StringBuilder sb = new StringBuilder();
		sb.append("[MATCH: ").append(result ? "TRUE" : "FALSE").append("] ==> ");
		StringJoiner joiner = new StringJoiner(", ");
		for (Map.Entry<FILE_ATTRIBUTES, Object> entry : this.attributes.entrySet()) {
			Object value = entry.getValue();
			joiner.add(entry.getKey().name() + ": " + (Objects.isNull(value) ? "NULL" : value));
		}
		return sb.append(joiner.toString()).toString();
	}
	
	public Object accessAndGetAttributes(FILE_ATTRIBUTES attr) {
		if (!this.attributes.containsKey(attr)) {
			this.attributes.put(attr, this.calculateAttribute(attr));
		}
		return this.attributes.get(attr);
	}
	
	private Object calculateAttribute(FILE_ATTRIBUTES attr) {
		switch (attr) {
			case NAME:
				return this.name;
			case EXTENSION:
				return this.isDirectory ? null : FileUtils.getFileExtension(this.name);
			case SIZE:
				return this.isDirectory ? null : attrs.size();
			case IS_DIRECTORY:
				return this.isDirectory;
			case IS_FILE:
				return !this.isDirectory;
			case IS_DIRECTORY_EMPTY:
				try {
					return this.isDirectory ? FileUtils.isDirectoryEmpty(this.path) : false;
				} catch (IOException e) {
					return false;
				}
			default: 
				if (attr.name().startsWith("CREATION")) {
					if (Objects.isNull(this.created)) {
						this.created = LocalDateTime.ofInstant(this.attrs.creationTime().toInstant(), ZoneId.systemDefault());
					}
					return this.calculateChronoUnit(this.created, this.now, attr);
				} else if (attr.name().startsWith("MODIFIED")) {
					if (Objects.isNull(this.modified)) {
						this.modified = LocalDateTime.ofInstant(this.attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault());
					}
					return this.calculateChronoUnit(this.modified, this.now, attr);
				} else if (attr.name().startsWith("NAME_EXPRESSION")) {
					if (Objects.isNull(this.dateTimeBaseOnName)) return null;
					return this.calculateChronoUnit(this.dateTimeBaseOnName, created, attr);
				}
		}
		return null;
	}
	
	private Long calculateChronoUnit(LocalDateTime from, LocalDateTime to, FILE_ATTRIBUTES attr) {
		String type = attr.name();
		if (type.endsWith("_MINUTES_AGE")) return ChronoUnit.MINUTES.between(from, to);
		if (type.endsWith("_HOURS_AGE")) return ChronoUnit.HOURS.between(from, to);
		if (type.endsWith("_DAYS_AGE")) return ChronoUnit.DAYS.between(from, to);
		if (type.endsWith("_WEEKS_AGE")) return ChronoUnit.WEEKS.between(from, to);
		if (type.endsWith("_MONTHS_AGE")) return ChronoUnit.MONTHS.between(from, to);
		if (type.endsWith("_YEARS_AGE")) return ChronoUnit.YEARS.between(from, to);
		return null;
	}
	
	private LocalDateTime extractDateTimeFromPath(String path, String pattern) {
		if (StringUtils.isNullOrEmpty(path) || StringUtils.isNullOrEmpty(pattern)) return null;
		String normalizedPath = path.replace("\\", "/");
		String normalizedPattern = pattern.replace("\\", "/");
		
		Matcher matcher = PLACEHOLDER_PATTERN.matcher(normalizedPattern);
		StringBuffer regex = new StringBuffer();
		List<String> formatList = new ArrayList<String>();
		while (matcher.find()) {
			String formatMatcher = matcher.group();
			formatList.add(formatMatcher);
			matcher.appendReplacement(regex, Matcher.quoteReplacement("(\\d{" + formatMatcher.length() + "})"));
		}
		matcher.appendTail(regex);
		
		if (formatList.isEmpty()) return null;
		
		Matcher pathMatcher = Pattern.compile("^" + regex.toString().replace("\\", "\\\\") + "$").matcher(normalizedPath);
		
		if (pathMatcher.matches()) {
			StringBuilder extractedValue = new StringBuilder();
			StringBuilder finalFormat = new StringBuilder();
			
			for (int i = 0; i < formatList.size(); i++) {
				extractedValue.append(pathMatcher.group(i + 1));
				finalFormat.append(formatList.get(i));
			}
			
			try {
				DateTimeFormatter formatter = new DateTimeFormatterBuilder().appendPattern(finalFormat.toString())
																			.parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
																			.parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
																			.parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
																			.parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
																			.parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
																			.toFormatter();
				return LocalDateTime.parse(extractedValue.toString(), formatter);
			} catch (Exception e) {
				return null;
			}
		}
		return null;
	}
}