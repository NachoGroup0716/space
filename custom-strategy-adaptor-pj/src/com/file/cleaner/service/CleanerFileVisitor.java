package com.file.cleaner.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.file.cleaner.data.CleanerFileData;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CleanerFileVisitor extends SimpleFileVisitor<Path> {
	private Expression expression;
	private LocalDateTime now;
	private Path baseDirectory;
	private String pattern;
	private BufferedWriter historyWriter;

	public CleanerFileVisitor(Expression expression, LocalDateTime now, Path baseDirectory, String pattern, BufferedWriter historyWriter) {
		this.expression = expression;
		this.now = now;
		this.baseDirectory = baseDirectory;
		this.pattern = pattern;
		this.historyWriter = historyWriter;
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		if (!baseDirectory.normalize().toString().equals(dir.normalize().toString())) {
			try {
				LocalDateTime dateTimeFromName = this.extractDateTimeFromPathName(dir.toAbsolutePath().toString(), pattern);
				CleanerFileData data = new CleanerFileData(dir, Files.readAttributes(dir, BasicFileAttributes.class), now, dateTimeFromName);
				StandardEvaluationContext context = new StandardEvaluationContext(data);
				context.addPropertyAccessor(new CleanerEnumAccessor());
				Boolean isMatch = expression.getValue(context, Boolean.class);
				log.debug("{}", data.toString(isMatch));
				if (isMatch) {
					delete(dir);
				}
			} catch (Exception e) {
				log.error("Failed to check directory :: {}\r\n", dir.toAbsolutePath(), e);
			}
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		try {
			CleanerFileData data = new CleanerFileData(file, attrs, now, null);
			StandardEvaluationContext context = new StandardEvaluationContext(data);
			context.addPropertyAccessor(new CleanerEnumAccessor());
			Boolean isMatch = expression.getValue(context, Boolean.class);
			log.debug("{}", data.toString(isMatch));
			if (isMatch) {
				delete(file);
			}
		} catch (Exception e) {
			log.error("Failed to check file :: {}\r\n", file.toAbsolutePath(), e);
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		log.error("Failed to visit file :: {}\r\n", file.toAbsolutePath(), exc);
		return FileVisitResult.CONTINUE;
	}
	
	private void write(boolean isDirectory, Path path) throws IOException {
		if (Objects.nonNull(this.historyWriter)) {
			this.historyWriter.append(isDirectory ? "D" : "F").append(":").append(path.toAbsolutePath().toString());
			this.historyWriter.newLine();
		}
	}
	
	private void delete(Path path) throws IOException {
		if (path == null || Files.notExists(path)) return;
		
		if (Files.isDirectory(path)) {
			try (Stream<Path> walk = Files.walk(path)) {
				walk.sorted(Comparator.reverseOrder())
					.forEach(item -> {
						try {
							Files.deleteIfExists(item);
							log.debug("DELETED :: {}", item.toAbsolutePath());
						} catch (Exception e) {
							log.error("FAILED TO DELETE :: {}\r\n", item.toAbsolutePath(), e);
						}
					});
			}
		} else {
			Files.deleteIfExists(path);
			log.debug("DELETED :: {}", path.toAbsolutePath());
		}
	}
	
	private LocalDateTime extractDateTimeFromPathName(String path, String pattern) {
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
