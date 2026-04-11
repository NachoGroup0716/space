package com.file.cleaner;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;

import org.springframework.expression.Expression;

import com.file.cleaner.data.CleanerFileData;
import com.file.cleaner.data.CleanerInterfaceInfo;
import com.file.cleaner.service.CleanerFileVisitor;
import com.file.cleaner.utils.FileUtils;
import com.file.cleaner.utils.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Cleaner {
	private List<String> excludePathList;
	
	public void run(CleanerInterfaceInfo info) throws Exception {
		LocalDateTime now = LocalDateTime.now();
		Expression expression = info.getExpression();
		String historyPath = info.getHistoryPath();
		String searchPaths = info.getSearchPaths();
		String searchRule = info.getSearchRules();
		
		if (StringUtils.isNullOrEmpty(historyPath)) {
			loop(now, null, expression, searchPaths, searchRule);
		} else {
			Path historyFile = Paths.get(historyPath).resolve("history_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")));
			Files.createDirectories(historyFile.getParent());
			try (BufferedWriter history = Files.newBufferedWriter(historyFile)) {
				loop(now, history, expression, searchPaths, searchRule);
				history.flush();
				history.close();
			} catch (Exception e) {
				log.error("삭제 이력 파일 생성 중 오류가 발생했습니다. [HISTORY_PATH: {}]\r\n", historyFile.toAbsolutePath(), e);
			}
			if (Files.exists(historyFile) && Files.size(historyFile) == 0L) {
				log.debug("삭제 이력이 없으므로 이력 파일을 삭제 합니다. [HISTORY_PATH: {}]", historyFile.toAbsolutePath());
				Files.deleteIfExists(historyFile);
			}
		}
	}
	
	private void loop(LocalDateTime now, BufferedWriter history, Expression expression, String searchPaths, String searchRule) throws IOException {
		String[] searchPathArr = searchPaths.split(",");
		for (String searchPath : searchPathArr) {
			String normalizedPath = FileUtils.normalizePath(searchPath);
			String normalizedBasePath = extractBasePath(normalizedPath);
			Path basePath = Paths.get(normalizedBasePath);
			if (Files.isDirectory(basePath)) {
				CleanerFileVisitor visitor = null;
				if (normalizedPath.equals(normalizedBasePath)) {
					log.info("삭제 프로세스 시작 :: [BASE_PATH: {}][RULE: {}]", normalizedPath, searchRule);
					visitor = new CleanerFileVisitor(excludePathList, history, now, expression, null, basePath);
				} else {
					log.info("삭제 프로세스 시작 :: [BASE_PATH: {}][RULE: {}][PATTERN: {}]", normalizedPath, searchRule, normalizedBasePath);
					visitor = new CleanerFileVisitor(excludePathList, history, now, expression, normalizedBasePath, basePath);
				}
				try {
					Files.walkFileTree(basePath, visitor);
				} catch (Exception e) {
					log.error("디렉토리 확인 중 오류가 발생했습니다. :: [BASE_PATH: {}]", basePath.toAbsolutePath());
				}
				
			} else {
				log.error("지정된 경로가 존재하지 않거나 디렉토리가 아닙니다. :: [BASE_PATH: {}]", normalizedBasePath);
			}
		}
	}
	
	private String extractBasePath(String path) {
		Matcher matcher = CleanerFileData.PLACEHOLDER_PATTERN.matcher(path);
		if (matcher.find()) {
			return path.substring(0, matcher.start());
		} else {
			return path;
		}
	}
}
