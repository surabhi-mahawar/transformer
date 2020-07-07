package com.samagra.transformer.odk;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FormNameUtils {

    private static final String CONTROL_CHAR_REGEX = "[\\p{Cntrl}]";
    private static final Pattern CONTROL_CHAR_PATTERN = Pattern.compile(CONTROL_CHAR_REGEX);

    private FormNameUtils() {
    }

    public static String normalizeFormName(String formName, boolean returnNullIfNothingChanged) {
        if (formName == null) {
            return null;
        }

        Matcher matcher = CONTROL_CHAR_PATTERN.matcher(formName);
        return matcher.find()
                ? matcher.replaceAll(" ")
                : (returnNullIfNothingChanged ? null : formName);
    }

    // Create a sanitized filename prefix from the given form name. No extension is added.
    public static String formatFilenameFromFormName(String formName) {
        if (formName == null) {
            return null;
        }
        // Keep alphanumerics, replace others with a space
        String fileName = formName.replaceAll("[^\\p{L}\\p{Digit}]", " ");
        // Replace consecutive whitespace characters with single space
        fileName = fileName.replaceAll("\\p{javaWhitespace}+", " ");
        return fileName.trim();
    }
}
