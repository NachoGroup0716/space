package com.file.cleaner.data;

import java.io.IOException;
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
import com.file.cleaner.utils.FileUtils;

public class CleanerFileData {
	public static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("@\\{([A-Za-z]+)\\}");
	private final EnumMap<FILE_ATTRIBUTES, Object> attributes = new EnumMap<FILE_ATTRIBUTES, Object>(FILE_ATTRIBUTES.class);
	private final Set<FILE_ATTRIBUTES> accessedAttributes = EnumSet.noneOf(FILE_ATTRIBUTES.class);
	
	public CleanerFileData(Path path, BasicFileAttributes attr, LocalDateTime now, LocalDateTime dateTimeBaseOnName) throws IOException {
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
			this.attributes.put(FILE_ATTRIBUTES.IS_DIRECTORY_EMPTY, FileUtils.isDirectoryEmpty(path));
		} else {
			this.attributes.put(FILE_ATTRIBUTES.IS_DIRECTORY, false);
			this.attributes.put(FILE_ATTRIBUTES.IS_FILE, true);
			this.attributes.put(FILE_ATTRIBUTES.SIZE, attr.size());
			this.attributes.put(FILE_ATTRIBUTES.EXTENSION, FileUtils.getFileExtension(name));
		}
		if (Objects.nonNull(dateTimeBaseOnName)) {
			this.attributes.put(FILE_ATTRIBUTES.NAME_EXPRESSION_MINUTES_AGE, ChronoUnit.MINUTES.between(dateTimeBaseOnName, now));
			this.attributes.put(FILE_ATTRIBUTES.NAME_EXPRESSION_HOURS_AGE, ChronoUnit.HOURS.between(dateTimeBaseOnName, now));
			this.attributes.put(FILE_ATTRIBUTES.NAME_EXPRESSION_DAYS_AGE, ChronoUnit.DAYS.between(dateTimeBaseOnName, now));
			this.attributes.put(FILE_ATTRIBUTES.NAME_EXPRESSION_WEEKS_AGE, ChronoUnit.WEEKS.between(dateTimeBaseOnName, now));
			this.attributes.put(FILE_ATTRIBUTES.NAME_EXPRESSION_MONTHS_AGE, ChronoUnit.MONTHS.between(dateTimeBaseOnName, now));
			this.attributes.put(FILE_ATTRIBUTES.NAME_EXPRESSION_YEARS_AGE, ChronoUnit.YEARS.between(dateTimeBaseOnName, now));
		}
	}
	
	public Object accessAndGetAttributes(FILE_ATTRIBUTES attr) {
		this.accessedAttributes.add(attr);
		return this.attributes.get(attr);
	}
	
	public Object getAttributes(FILE_ATTRIBUTES attr) {
		return this.attributes.get(attr);
	}
	
	public boolean containsKey(FILE_ATTRIBUTES attr) {
		return this.attributes.containsKey(attr);
	}
	
	public EnumMap<FILE_ATTRIBUTES, Object> getAttributes() {
		return this.attributes;
	}
	
	public Set<FILE_ATTRIBUTES> getAccessedAttributes() {
		return accessedAttributes;
	}
	
	public String toString(boolean result) {
		StringBuilder sb = new StringBuilder();
		sb.append("MATCH: ").append(String.valueOf(result).toUpperCase()).append(" ==> ");
		for (FILE_ATTRIBUTES attr : this.accessedAttributes) {
			sb.append(attr.name()).append(":");
			Object value = this.attributes.get(attr);
			sb.append(Objects.isNull(value) ? "NULL" : String.valueOf(value)).append(", ");
		}
		
		String line = sb.toString().trim();
		if (line.endsWith(",")) {
			line = line.substring(0, line.length() - 1);
		}
		return line;
	}
}