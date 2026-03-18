package com.file.cleaner.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.regex.Matcher;

import com.file.cleaner.data.CleanerFileData;
import com.file.cleaner.data.CleanerInterfaceInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CleanerService {
	public void onSignal(CleanerInterfaceInfo info) {
		LocalDateTime now = LocalDateTime.now();
		String[] searchPaths = info.getSearchPaths().split(",");
		Path history = Paths.get("some path");
		try (BufferedWriter writer = Files.newBufferedWriter(history)) {
			for (String path : searchPaths) {
				String normalizedPath = extractBasePath(path);
				Path baseDirectory = Paths.get(normalizedPath);
				try {
					Files.walkFileTree(baseDirectory, new CleanerFileVisitor(info.getExpression(), now, baseDirectory, path, writer));
				} catch (IOException e) {
					log.error("{} 경로 확인 중 오류가 발생했습니다.\r\n", baseDirectory.toAbsolutePath(), e);
				}
			}
		} catch (Exception e) {
			log.error("히스토리 파일 생성에 실패했습니다.");
		}
	}
	
	private String extractBasePath(String path) {
		String normalizedPath = path.replace("\\", "/");
		Matcher matcher = CleanerFileData.PLACEHOLDER_PATTERN.matcher(normalizedPath);
		if (matcher.find()) {
			return normalizedPath.substring(0, matcher.start());
		} else {
			return normalizedPath;
		}
	}
}
