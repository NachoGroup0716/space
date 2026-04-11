package com.file;

import java.util.Collections;

import com.file.cleaner.Cleaner;
import com.file.cleaner.data.CleanerInterfaceInfo;

public class RunCleaner {

	public static void main(String[] args) throws Exception {
		CleanerInterfaceInfo info = new CleanerInterfaceInfo();
		info.setSearchPaths("/TEST/test/old/file");
		info.setSearchRules("(IS_FILE and CREATION_DAYS_AGE > 30) or (IS_DIRECTORY and (CREATION_DAYS_AGE > 30 or IS_DIRECTORY_EMPTY))");
		info.setHistoryPath("/TEST/history");
		info.setExcludePath(Collections.singletonList("/TEST/test/old/file/20260301"));
		
		Cleaner cleaner = new Cleaner();
		cleaner.run(info);
	}

}
