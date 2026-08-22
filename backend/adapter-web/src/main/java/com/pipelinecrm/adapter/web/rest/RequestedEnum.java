package com.pipelinecrm.adapter.web.rest;

import com.pipelinecrm.adapter.web.error.MalformedRequest;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Turns a word from a request into one of the domain's constants.
 *
 * <p>The list of legal values is never repeated here: it is read from the enum, so a new deal
 * stage becomes accepted by the API the moment the domain accepts it. A word that is not one
 * of them is a malformed request — 400 — rather than a server fault.
 */
public final class RequestedEnum {

    private RequestedEnum() {
    }

    public static <E extends Enum<E>> E of(Class<E> type, String word, String field) {
        try {
            return Enum.valueOf(type, word.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException notOneOfThem) {
            throw new MalformedRequest(field + " must be one of " + legalValuesOf(type) + ", was: " + word);
        }
    }

    private static <E extends Enum<E>> String legalValuesOf(Class<E> type) {
        return Arrays.stream(type.getEnumConstants()).map(Enum::name).collect(Collectors.joining(", "));
    }
}
