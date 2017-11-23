/*
 * Copyright (c) 2016 Kevin Herron
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *   http://www.eclipse.org/org/documents/edl-v10.html.
 */

package org.eclipse.milo.opcua.stack.core.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;

public class FutureUtils {

    public static <T> CompletableFuture<List<T>> sequence(Stream<CompletableFuture<T>> stream) {
        return sequence(stream.collect(Collectors.toList()));
    }

    public static <T> CompletableFuture<List<T>> sequence(List<CompletableFuture<T>> futures) {
        if (futures.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        CompletableFuture[] fa = futures.toArray(new CompletableFuture[futures.size()]);

        return CompletableFuture.allOf(fa).thenApply(v -> {
            List<T> results = new ArrayList<>(futures.size());

            for (CompletableFuture<T> cf : futures) {
                results.add(cf.join());
            }

            return results;
        });
    }

    public static <T> CompletableFuture<List<T>> sequence(CompletableFuture<T>[] futures) {
        if (futures.length == 0) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        return CompletableFuture.allOf(futures).thenApply(v -> {
            List<T> results = new ArrayList<>(futures.length);

            for (CompletableFuture<T> cf : futures) {
                results.add(cf.join());
            }

            return results;
        });
    }

    /**
     * Return a {@link CompletableFuture} that has been completed exceptionally using the provided {@link Throwable}.
     *
     * @param ex the {@link Throwable} to complete with.
     * @return a {@link CompletableFuture} that has been completed exceptionally using the provided {@link Throwable}.
     */
    public static <T> CompletableFuture<T> failedFuture(Throwable ex) {
        CompletableFuture<T> f = new CompletableFuture<>();
        f.completeExceptionally(ex);
        return f;
    }

    /**
     * Return a {@link CompletableFuture} that has been completed exceptionally with a {@link UaException} built from
     * {@code statusCode}.
     *
     * @param statusCode the status code to build the {@link UaException} with.
     * @return a {@link CompletableFuture} that has been completed exceptionally with a {@link UaException} built from
     * {@code statusCode}.
     */
    public static <T> CompletableFuture<T> failedUaFuture(long statusCode) {
        return failedUaFuture(new StatusCode(statusCode));
    }

    /**
     * Return a {@link CompletableFuture} that has been completed exceptionally with a {@link UaException} built from
     * {@code statusCode}.
     *
     * @param statusCode the {@link StatusCode} to build the {@link UaException} with.
     * @return a {@link CompletableFuture} that has been completed exceptionally with a {@link UaException} built from
     * {@code statusCode}.
     */
    public static <T> CompletableFuture<T> failedUaFuture(StatusCode statusCode) {
        return failedFuture(new UaException(statusCode));
    }

    /**
     * Return a {@link CompletableFuture} that has been completed exceptionally with a {@link UaException} built from
     * {@code statusCode}.
     *
     * @param cause      the inner {@link Exception}.
     * @param statusCode the status code to build the {@link UaException} with.
     * @return a {@link CompletableFuture} that has been completed exceptionally with a {@link UaException} built from
     * {@code statusCode}.
     */
    public static <T> CompletableFuture<T> failedUaFuture(Throwable cause, long statusCode) {
        return failedFuture(new UaException(statusCode, cause));
    }

    /**
     * Complete {@code future} with the result of another {@link CompletableFuture} that is provided to the returned
     * {@link CompletionBuilder}.
     *
     * @param future the future to complete.
     * @return a {@link CompletionBuilder}.
     */
    public static <T> CompletionBuilder<T> complete(CompletableFuture<T> future) {
        return new CompletionBuilder<>(future);
    }

    public static final class CompletionBuilder<T> {

        private final CompletableFuture<T> toComplete;

        private CompletionBuilder(CompletableFuture<T> toComplete) {
            this.toComplete = toComplete;
        }

        /**
         * Complete the contained to-be-completed {@link CompletableFuture} using the result of {@code future}.
         *
         * @param future the {@link CompletableFuture} to use as the result for the contained future.
         * @return the original, to-be-completed future provided to this {@link CompletionBuilder}.
         */
        public CompletableFuture<T> with(CompletableFuture<T> future) {
            future.whenComplete((v, ex) -> {
                if (ex != null) toComplete.completeExceptionally(ex);
                else toComplete.complete(v);
            });

            return toComplete;
        }

        /**
         * Complete the contained to-be-completed {@link CompletableFuture} using the result of {@code future}
         * asynchronously using the provided {@link Executor}.
         *
         * @param future   the {@link CompletableFuture} to use as the result for the contained future.
         * @param executor the {@link Executor} to use.
         * @return the original, to-be-completed future provided to this {@link CompletionBuilder}.
         */
        public CompletableFuture<T> withAsync(CompletableFuture<T> future, Executor executor) {
            future.whenCompleteAsync((v, ex) -> {
                if (ex != null) toComplete.completeExceptionally(ex);
                else toComplete.complete(v);
            }, executor);

            return toComplete;
        }

    }

}
