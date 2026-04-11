package com.file.cleaner.utils;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import com.file.cleaner.utils.constants.FileNameEnum;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileUtils {
	private final static String DOT = ".";
	
	public static String normalizePath(String path) {
		return path.replace("\\", "/");
	}
	
	public static boolean isDirectoryEmpty(Path dir) throws IOException {
		if (Files.isDirectory(dir)) {
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
				return !stream.iterator().hasNext();
			}
		}
		return false;
	}
	
	public static String getFileBaseName(String path) {
		Map<FileNameEnum, String> result = getFileNameInfo(path);
		if (Objects.isNull(result)) {
			return null;
		} else {
			return result.get(FileNameEnum.BASE_NAME);
		}
	}
	
	public static String getFileExtension(String path) {
		Map<FileNameEnum, String> result = getFileNameInfo(path);
		if (Objects.isNull(result)) {
			return null;
		} else {
			return result.get(FileNameEnum.EXTENSION);
		}
	}
	
	public static Map<FileNameEnum, String> getFileNameInfo(String path) {
		if (StringUtils.isNullOrEmpty(path)) return null;
		Map<FileNameEnum, String> result = new HashMap<FileNameEnum, String>();
		String fullName = Paths.get(path).getFileName().toString();
		String baseName = null;
		String extension = null;
		int dot = fullName.lastIndexOf(DOT);
		if (dot > 0 && dot < fullName.length()) {
			baseName = fullName.substring(0, dot);
			extension = fullName.substring(dot + 1);
		} else {
			baseName = fullName;
			extension = "";
		}
		result.put(FileNameEnum.BASE_NAME, baseName);
		result.put(FileNameEnum.EXTENSION, extension);
		return result;
	}
	
	public static void delete(Path path) throws IOException {
		if (path == null || Files.notExists(path)) return;
		
		if (Files.isDirectory(path)) {
			try (Stream<Path> stream = Files.walk(path)) {
				Iterator<Path> iterator = stream.sorted(Comparator.reverseOrder()).iterator();
				while (iterator.hasNext()) {
					Path file = iterator.next();
					Files.delete(file);
					log.debug("DELETED :: {}", file.toAbsolutePath());
				}
			}
		} else {
			Files.delete(path);
			log.debug("DELETED :: {}", path.toAbsolutePath());
		}
	}
}
