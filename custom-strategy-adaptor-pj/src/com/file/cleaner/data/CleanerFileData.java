package com.file.cleaner.data;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import com.file.cleaner.constants.FILE_ATTRIBUTES;

public class CleanerFileData {
	public static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("@\\{([A-Za-z]+)\\}");
	private final EnumMap<FILE_ATTRIBUTES, Object> attributes = new EnumMap<FILE_ATTRIBUTES, Object>(FILE_ATTRIBUTES.class);
	private final Set<FILE_ATTRIBUTES> accessedAttributes = EnumSet.noneOf(FILE_ATTRIBUTES.class);
	
	public CleanerFileData(Path path, BasicFileAttributes attr, LocalDateTime now, LocalDateTime dateTimeBasedOnName) throws Exception {
		String name = path.getFileName().toString();
		LocalDateTime created = LocalDateTime.ofInstant(attr.creationTime().toInstant(), ZoneId.systemDefault());
		LocalDateTime modified = LocalDateTime.ofInstant(attr.lastModifiedTime().toInstant(), ZoneId.systemDefault());
		this.attributes.put(FILE_ATTRIBUTES.NAME, name);
		this.attributes.put(FILE_ATTRIBUTES.CREATION_MINUTES_AGE, ChronoUnit.MINUTES.between(created, now));
		this.attributes.put(FILE_ATTRIBUTES.CREATION_HOURS_AGE, ChronoUnit.HOURS.between(created, now));
		this.attributes.put(FILE_ATTRIBUTES.CREATION_DAYS_AGE, ChronoUnit.DAYS.between(created, now));
		this.attributes.put(FILE_ATTRIBUTES.CREATION_WEEKS_AGE, ChronoUnit.WEEKS.between(created, now));
		this.attributes.put(FILE_ATTRIBUTES.CREATION_MONTHS_AGE, ChronoUnit.MONTHS.between(created, now));
		this.attributes.put(FILE_ATTRIBUTES.CREATION_YEARS_AGE, ChronoUnit.YEARS.between(created, now));
		this.attributes.put(FILE_ATTRIBUTES.MODIFIED_MINUTES_AGE, ChronoUnit.MINUTES.between(modified, now));
		this.attributes.put(FILE_ATTRIBUTES.MODIFIED_HOURS_AGE, ChronoUnit.HOURS.between(modified, now));
		this.attributes.put(FILE_ATTRIBUTES.MODIFIED_DAYS_AGE, ChronoUnit.DAYS.between(modified, now));
		this.attributes.put(FILE_ATTRIBUTES.MODIFIED_WEEKS_AGE, ChronoUnit.WEEKS.between(modified, now));
		this.attributes.put(FILE_ATTRIBUTES.MODIFIED_MONTHS_AGE, ChronoUnit.MONTHS.between(modified, now));
		this.attributes.put(FILE_ATTRIBUTES.MODIFIED_YEARS_AGE, ChronoUnit.YEARS.between(modified, now));
		if (Files.isDirectory(path)) {
			this.attributes.put(FILE_ATTRIBUTES.IS_DIRECTORY, true);
			this.attributes.put(FILE_ATTRIBUTES.IS_FILE, false);
			this.attributes.put(FILE_ATTRIBUTES.IS_DIRECTORY_EMPTY, this.isDirectoryEmpty(path));
		} else {
			this.attributes.put(FILE_ATTRIBUTES.IS_DIRECTORY, false);
			this.attributes.put(FILE_ATTRIBUTES.IS_FILE, true);
			this.attributes.put(FILE_ATTRIBUTES.SIZE, attr.size());
			this.attributes.put(FILE_ATTRIBUTES.EXTENSION, this.getFileExtension(name));
		}
		if (Objects.nonNull(dateTimeBasedOnName)) {
			this.attributes.put(FILE_ATTRIBUTES.NAME_MINUTES_AGE, ChronoUnit.MINUTES.between(dateTimeBasedOnName, now));
			this.attributes.put(FILE_ATTRIBUTES.NAME_HOURS_AGE, ChronoUnit.HOURS.between(dateTimeBasedOnName, now));
			this.attributes.put(FILE_ATTRIBUTES.NAME_DAYS_AGE, ChronoUnit.DAYS.between(dateTimeBasedOnName, now));
			this.attributes.put(FILE_ATTRIBUTES.NAME_WEEKS_AGE, ChronoUnit.WEEKS.between(dateTimeBasedOnName, now));
			this.attributes.put(FILE_ATTRIBUTES.NAME_MONTHS_AGE, ChronoUnit.MONTHS.between(dateTimeBasedOnName, now));
			this.attributes.put(FILE_ATTRIBUTES.NAME_YEARS_AGE, ChronoUnit.YEARS.between(dateTimeBasedOnName, now));
		}
	}
	
	private boolean isDirectoryEmpty(Path path) throws IOException {
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
			return !stream.iterator().hasNext();
		}
	}
	
	private String getFileExtension(String name) {
		int dot = name.lastIndexOf(".");
		if (dot > 0 && dot < name.length()) {
			return name.substring(dot + 1);
		} else {
			return "";
		}
	}
	
	public boolean containsKey(FILE_ATTRIBUTES attr) {
		return this.attributes.containsKey(attr);
	}
	
	public Object get(FILE_ATTRIBUTES attr) {
		return this.attributes.get(attr);
	}
	
	public Object accessAndGet(FILE_ATTRIBUTES attr) {
		this.accessedAttributes.add(attr);
		return this.attributes.get(attr);
	}

	public EnumMap<FILE_ATTRIBUTES, Object> getAttributes() {
		return attributes;
	}

	public Set<FILE_ATTRIBUTES> getAccessedAttributes() {
		return accessedAttributes;
	}
	
	public String toString(boolean result) {
		StringBuffer sb = new StringBuffer();
		sb.append("MATCH: ").append(String.valueOf(result).toUpperCase()).append(" ==> ");
		for (FILE_ATTRIBUTES attr : accessedAttributes) {
			sb.append(attr.name()).append(":");
			if (this.attributes.containsKey(attr)) {
				sb.append(this.attributes.get(attr));
			} else {
				sb.append("NULL");
			}
			sb.append(", ");
		}
		
		String line = sb.toString().trim();
		if (line.endsWith(",")) {
			line = line.substring(0, line.length() - 1);
		}
		return line;
	}
}