package com.adaptor.custom.utils.constants;

import java.util.Objects;
import java.util.function.Predicate;

public enum CheckOptions {
	IS_NULL(Objects::isNull),
	IS_NOT_NULL(item -> !Objects.isNull(item)),
	IS_STRING(item -> item instanceof String),
	IS_NOT_STRING(item -> !(item instanceof String)),
	IS_EMPTY(item -> IS_NOT_NULL.check(item) && IS_STRING.check(item) && String.valueOf(item).isEmpty()),
	IS_NOT_EMPTY(item -> IS_NOT_NULL.check(item) && IS_STRING.check(item) && !String.valueOf(item).isEmpty()),
	IS_DIGIT(item -> IS_NOT_EMPTY.check(item) && String.valueOf(item).chars().allMatch(Character::isDigit)),
	IS_NOT_DIGIT(item -> IS_NOT_EMPTY.check(item) && !String.valueOf(item).chars().allMatch(Character::isDigit)),
	IS_ALPHABET(item -> IS_NOT_EMPTY.check(item) && String.valueOf(item).chars().allMatch(Character::isAlphabetic)),
	IS_NOT_ALPHABET(item -> IS_NOT_EMPTY.check(item) && !String.valueOf(item).chars().allMatch(Character::isAlphabetic))
	;
	
	private final Predicate<Object> validator;
	
	private CheckOptions(Predicate<Object> validator) {
		this.validator = validator;
	}
	
	public boolean check(Object item) {
		return validator.test(item);
	}
}

