package com.example.chestbot.util;

public final class TextSanitizer {

    private TextSanitizer() {
    }

    public static String stripEmoji(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        StringBuilder builder = new StringBuilder(value.length());
        value.codePoints().forEach(codePoint -> {
            if (!isBlockedCodePoint(codePoint)) {
                builder.appendCodePoint(codePoint);
            }
        });
        return builder.toString();
    }

    private static boolean isBlockedCodePoint(int codePoint) {
        return Character.isSupplementaryCodePoint(codePoint)
                || codePoint == 0xFE0F
                || codePoint == 0x200D
                || codePoint == 0x20E3;
    }
}
