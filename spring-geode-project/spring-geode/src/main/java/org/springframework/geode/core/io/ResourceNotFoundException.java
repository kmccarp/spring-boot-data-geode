/*
 * Copyright 2017-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.springframework.geode.core.io;

import org.springframework.core.io.Resource;

/**
 * {@link RuntimeException} indication that a {@link Resource} could not be found.
 *
 * @author John Blum
 * @see java.lang.RuntimeException
 * @see org.springframework.core.io.Resource
 * @since 1.3.1
 */
@SuppressWarnings("unused")
public class ResourceNotFoundException extends RuntimeException {

	/**
	 * Constructs a new instance of {@link ResourceNotFoundException} with no {@link String message}
	 * or known {@link Throwable cause}.
	 */
	public ResourceNotFoundException() {
	}

	/**
	 * Constructs a new instance of {@link ResourceNotFoundException} initialized with the given {@link String message}
	 * to describe the error.
	 *
	 * @param message {@link String} describing the {@link RuntimeException}.
	 */
	public ResourceNotFoundException(String message) {
		super(message);
	}

	/**
	 * Constructs a new instance of {@link ResourceNotFoundException} initialized with the given {@link Throwable}
	 * signifying the underlying cause of this {@link RuntimeException}.
	 *
	 * @param cause {@link Throwable} signifying the underlying cause of this {@link RuntimeException}.
	 * @see java.lang.Throwable
	 */
	public ResourceNotFoundException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructs a new instance of {@link ResourceNotFoundException} initialized with the given {@link String message}
	 * describing the error along with a {@link Throwable} signifying the underlying cause of this
	 * {@link RuntimeException}.
	 *
	 * @param message {@link String} describing the {@link RuntimeException}.
	 * @param cause {@link Throwable} signifying the underlying cause of this {@link RuntimeException}.
	 * @see java.lang.Throwable
	 */
	public ResourceNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
}
