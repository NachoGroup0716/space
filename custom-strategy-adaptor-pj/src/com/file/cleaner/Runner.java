package com.file.cleaner;

import com.file.cleaner.data.CleanerInterfaceInfo;
import com.file.cleaner.service.CleanerService;

public class Runner {

	public static void main(String[] args) throws Exception {
		CleanerInterfaceInfo info = new CleanerInterfaceInfo();
		info.setSearchPaths("/test/test/test/test/@{yyyy}/@{mm}/temp/@{dd}");
		info.setSearchRules("(IS_FILE and CREATION_DAYS_AGE > 60) or (IS_DIRECTORY and NAME_DAYS_AGE > 60)");
		info.afterPropertiesSet();
		
		CleanerService service = new CleanerService();
		service.onSignal(info);
	}

}
