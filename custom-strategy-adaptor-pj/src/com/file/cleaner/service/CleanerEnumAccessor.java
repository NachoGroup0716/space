package com.file.cleaner.service;

import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;

import com.file.cleaner.constants.FILE_ATTRIBUTES;
import com.file.cleaner.data.CleanerFileData;

public class CleanerEnumAccessor implements PropertyAccessor {
	@Override
	public Class<?>[] getSpecificTargetClasses() {
		return new Class[] { CleanerFileData.class };
	}

	@Override
	public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
		CleanerFileData data = (CleanerFileData) target;
		try {
			FILE_ATTRIBUTES attr = FILE_ATTRIBUTES.valueOf(name);
			return data.containsKey(attr);
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
		CleanerFileData data = (CleanerFileData) target;
		FILE_ATTRIBUTES attr = FILE_ATTRIBUTES.valueOf(name);
		return new TypedValue(data.accessAndGet(attr));
	}

	@Override
	public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
		return false;
	}

	@Override
	public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
		throw new UnsupportedOperationException("읽기 전용입니다.");
	}
}
