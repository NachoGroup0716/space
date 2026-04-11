package com.file.cleaner.data;

import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import com.file.cleaner.utils.StringUtils;

public class CleanerInterfaceInfo implements InitializingBean {
	private static final SpelExpressionParser PARSER = new SpelExpressionParser();
	private Expression expression;
	private String searchPaths;
	private String searchRules;
	private String historyPath;
	private List<String> excludePath;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		if (StringUtils.isNullOrEmpty(searchPaths)) throw new IllegalArgumentException("Property 'searchPaths' is required");
		if (StringUtils.isNullOrEmpty(searchRules)) throw new IllegalArgumentException("Property 'searchRules' is required");
		this.expression = PARSER.parseExpression(searchRules);
	}

	public String getSearchPaths() {
		return searchPaths;
	}

	public void setSearchPaths(String searchPaths) {
		this.searchPaths = searchPaths;
	}

	public String getSearchRules() {
		return searchRules;
	}

	public void setSearchRules(String searchRules) {
		this.searchRules = searchRules;
	}

	public String getHistoryPath() {
		return historyPath;
	}

	public void setHistoryPath(String historyPath) {
		this.historyPath = historyPath;
	}

	public List<String> getExcludePath() {
		return excludePath;
	}

	public void setExcludePath(List<String> excludePath) {
		this.excludePath = excludePath;
	}
	
	public Expression getExpression() {
		return expression;
	}
}
