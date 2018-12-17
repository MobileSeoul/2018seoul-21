package org.seoro.seoro.util;

import java.util.regex.Pattern;

public class ResponseErrorUtil {
    public static final Pattern MESSAGE_PARAMETER_PATTERN = Pattern.compile(".+\\((.+)\\)");
}
