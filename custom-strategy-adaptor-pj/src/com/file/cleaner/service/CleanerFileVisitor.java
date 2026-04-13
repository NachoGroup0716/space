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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
	private final StandardEvaluationContext CONTEXT = new StandardEvaluationContext();
	private Map<Path, String> targetDirectoryMap = new HashMap<Path, String>();
	private List<Path> excludeSubtreePaths = new ArrayList<Path>();
	private Set<Path> excludeExactPaths = new HashSet<Path>();
	private final BufferedWriter history;
	private final LocalDateTime now;
	private final Expression expression;
	private String pattern;
	private Path baseDirectory;
	private int fileCount = 0;
	private int directoryCount = 0;
	
	public CleanerFileVisitor(List<String> excludePathList, BufferedWriter history, LocalDateTime now, Expression expression, String pattern, Path baseDirectory) {
		this.baseDirectory = baseDirectory;
		if (Objects.nonNull(excludePathList)) {
			for (String excludePath : excludePathList) {
				if (StringUtils.isNullOrEmpty(excludePath)) continue;
				
				boolean isSubtree = excludePath.endsWith("/**") || excludePath.endsWith("/*");
				if (isSubtree) {
					excludePath = excludePath.substring(0, excludePath.lastIndexOf("/*"));
				}
				
				Path path = Paths.get(excludePath).toAbsolutePath();
				if (Files.exists(path)) {
					if (isSubtree) {
						this.excludeSubtreePaths.add(path);
					} else {
						this.excludeExactPaths.add(path);
					}
				}
			}
		}
		this.excludeExactPaths.add(this.baseDirectory.toAbsolutePath());
		this.history = history;
		this.now = now;
		this.expression = expression;
		this.pattern = pattern;
		this.CONTEXT.addPropertyAccessor(new CleanerEnumAccessor());
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		Path absolutePath = dir.toAbsolutePath();
		if (this.isExactExcluded(absolutePath)) {
			log.debug("EXCLUDE EXACT PATH :: {}", dir.toAbsolutePath());
			return FileVisitResult.CONTINUE;
		} else if (this.isSubtreeExcluded(absolutePath)) {
			log.debug("EXCLUDE SUBTREE PATH :: {}", dir.toAbsolutePath());
			return FileVisitResult.SKIP_SUBTREE;
		} else {
			CleanerFileData data = new CleanerFileData(dir, attrs, this.now, this.pattern);
			this.CONTEXT.setRootObject(data);
			boolean result = Boolean.TRUE.equals(expression.getValue(CONTEXT, Boolean.class));
			String message = data.toString(result);
			log.debug("{} {}", dir.getFileName(), message);
			if (result) {
				this.targetDirectoryMap.put(dir, message);
			}
		}
		return FileVisitResult.CONTINUE;
	}
	
	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		if (this.targetDirectoryMap.containsKey(dir)) {
			String message = this.targetDirectoryMap.get(dir);
			if (FileUtils.isDirectoryEmpty(dir)) {
				FileUtils.delete(dir);
				this.directoryCount++;
				this.write(true, dir, message);
			}
		}
		return FileVisitResult.CONTINUE;
	}
	
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		Path absolutePath = file.toAbsolutePath();
		if (this.isExactExcluded(absolutePath)) {
			log.debug("EXCLUDE EXACT PATH :: {}", file.toAbsolutePath());
			return FileVisitResult.CONTINUE;
		} else {
			CleanerFileData data = new CleanerFileData(file, attrs, now, pattern);
			CONTEXT.setRootObject(data);
			boolean result = Boolean.TRUE.equals(expression.getValue(CONTEXT, Boolean.class));
			String message = data.toString(result);
			log.debug("{} {}", file.getFileName(), message);
			if (result) {
				FileUtils.delete(file);
				this.fileCount++;
				this.write(false, file, message);
			}
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
	
	private boolean isExactExcluded(Path absolutePath) {
		return this.excludeExactPaths.contains(absolutePath);
	}
	
	private boolean isSubtreeExcluded(Path absolutePath) {
		for (Path subtree : this.excludeSubtreePaths) {
			if (absolutePath.startsWith(subtree)) return true;
		}
		return false;
	}
	
	public int getFileCount() {
		return this.fileCount;
	}
	
	public int getDirectoryCount() {
		return this.directoryCount;
	}
}
