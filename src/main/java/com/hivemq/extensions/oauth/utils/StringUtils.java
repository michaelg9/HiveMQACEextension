package com.hivemq.extensions.oauth.utils;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

public class StringUtils {

    @NotNull
    public static boolean isEmpty(@Nullable String text) {
        return text == null || "".equals(text);
    }
}
