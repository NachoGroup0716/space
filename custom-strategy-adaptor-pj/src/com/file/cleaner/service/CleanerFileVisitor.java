package com.file.cleaner.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.file.cleaner.data.CleanerFileData;
import com.file.cleaner.utils.FileUtils;
import com.file.cleaner.utils.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CleanerFileVisitor extends SimpleFileVisitor<Path> {
	private final CleanerEnumAccessor accessor = new CleanerEnumAccessor();
	private Map<Path, String> targetDirectoryMap = new HashMap<Path, String>();
	private List<Path> excludePathList;
	private BufferedWriter history;
	private LocalDateTime now;
	private Expression expression;
	private String pattern;
	
	public CleanerFileVisitor(List<String> excludePathList, BufferedWriter history, LocalDateTime now, Expression expression, String pattern, Path baseDirectory) throws IOException {
		super();
		if (excludePathList == null) {
			excludePathList = new ArrayList<String>();
		}
		excludePathList.add(baseDirectory.toAbsolutePath().toString());
		this.excludePathList = excludePathList.stream().map(path -> {
																		try {
																			return Paths.get(path).toRealPath();
																		} catch (Exception e) {
																			return Paths.get(path);
																		}
																	}).filter(path -> Objects.nonNull(path) && Files.exists(path)).collect(Collectors.toList());
		this.history = history;
		this.now = now;
		this.expression = expression;
		this.pattern = pattern;
		
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		try {
			Path realPath = dir.toRealPath();
			LocalDateTime dateTimeFromPath = this.extractDateTimeFromPath(dir.toAbsolutePath().toString(), pattern);
			CleanerFileData data = new CleanerFileData(dir, Files.readAttributes(dir, BasicFileAttributes.class), now, dateTimeFromPath);
			StandardEvaluationContext context = new StandardEvaluationContext(data);
			context.addPropertyAccessor(accessor);
			Boolean result = expression.getValue(context, Boolean.class);
			String message = data.toString(result);
			log.debug("{} {}", dir.getFileName(), message);
			if (result && !excludePathList.contains(realPath)) {
				targetDirectoryMap.put(dir, message);
			}
		} catch (Exception e) {
			log.error("Failed to check directory :: {}\r\n", dir.toAbsolutePath(), e);
		}
		return FileVisitResult.CONTINUE;
	}
	
	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		if (targetDirectoryMap.containsKey(dir)) {
			String message = targetDirectoryMap.get(dir);
			if (FileUtils.isDirectoryEmpty(dir)) {
				FileUtils.delete(dir);
				write(true, dir, message);
			}
			targetDirectoryMap.remove(dir);
		}
		return FileVisitResult.CONTINUE;
	}
	
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		try {
			Path realPath = file.toRealPath();
			LocalDateTime dateTimeFromPath = this.extractDateTimeFromPath(file.toAbsolutePath().toString(), pattern);
			CleanerFileData data = new CleanerFileData(file, attrs, now, dateTimeFromPath);
			StandardEvaluationContext context = new StandardEvaluationContext(data);
			context.addPropertyAccessor(accessor);
			Boolean result = expression.getValue(context, Boolean.class);
			String message = data.toString(result);
			if (result && !excludePathList.contains(realPath)) {
				FileUtils.delete(file);
				write(false, file, message);
			}
		} catch (Exception e) {
			log.error("Failed to check file :: {}\r\n", file.toAbsolutePath(), e);
		}
		return FileVisitResult.CONTINUE;
	}
	
	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		log.error("Failed to check file :: {}\r\n", file.toAbsolutePath(), exc);
		return FileVisitResult.CONTINUE;
	}
	
	private void write(boolean isDirectory, Path path, String message) throws IOException {
		if (Objects.nonNull(this.history)) {
			this.history.append(isDirectory ? "D" : "F").append(":").append(path.toAbsolutePath().toString());
			if (Objects.nonNull(message) && StringUtils.isNotNullAndEmpty(message)) {
				this.history.append(" :: ").append(message);
			}
			this.history.newLine();
		}
	}
	
	private LocalDateTime extractDateTimeFromPath(String path, String pattern) {
		if (StringUtils.isNullOrEmpty(path) || StringUtils.isNullOrEmpty(pattern)) return null;
		String normalizedPath = path.replace("\\", "/");
		String normalizedPattern = pattern.replace("\\", "/");
		
		Matcher matcher = CleanerFileData.PLACEHOLDER_PATTERN.matcher(normalizedPattern);
		StringBuffer regexBuffer = new StringBuffer();
		List<String> formatList = new ArrayList<String>();
		
		while (matcher.find()) {
			String formatMatcher = matcher.group(1);
			formatList.add(formatMatcher);
			matcher.appendReplacement(regexBuffer, "(\\\\d{" + formatMatcher.length() + "})");
		}
		matcher.appendTail(regexBuffer);
		
		if (formatList.isEmpty()) return null;
		
		Matcher pathMatcher = Pattern.compile("^" + regexBuffer.toString() + "$").matcher(normalizedPath);
		
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
