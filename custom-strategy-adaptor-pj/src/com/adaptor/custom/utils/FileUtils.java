package com.adaptor.custom.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import com.adaptor.custom.utils.constants.CheckOptions;
import com.adaptor.custom.utils.constants.FileNameEnum;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileUtils {
	private static final String SEPERATOR = File.separator;
	private static final String DOT = ".";
	
	public static String createFileName(String baseName, String extension) {
		return String.join(DOT, new String[] {baseName, extension});
	}
	
	public static Map<FileNameEnum, String> getFileNameInfo(String path) {
		if (StringUtils.checkAny(path, CheckOptions.IS_NULL, CheckOptions.IS_NOT_STRING)) return null;
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
			try (Stream<Path> walk = Files.walk(path)) {
				walk.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.forEach(file -> {
						file.delete();
						log.debug("DELETED :: {}", file.getAbsolutePath());
					});
			}
		} else {
			Files.deleteIfExists(path);
			log.debug("DELETED :: {}", path.toAbsolutePath());
		}
	}
}
