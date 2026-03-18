package com.file.cleaner.data;

import java.util.Objects;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

public class CleanerInterfaceInfo implements InitializingBean {
	private static final SpelExpressionParser PARSER = new SpelExpressionParser();
	private Expression expression;
	private String searchPaths;
	private String searchRules;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		if (Objects.isNull(searchPaths) || searchPaths.isEmpty()) {
			throw new IllegalArgumentException("Property 'searchPaths' is required");
		}
		if (Objects.isNull(searchRules) || searchRules.isEmpty()) {
			throw new IllegalArgumentException("Property 'searchRules' is required");
		}
		this.expression = PARSER.parseExpression(this.searchRules);
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

	public Expression getExpression() {
		return expression;
	}
	
}
