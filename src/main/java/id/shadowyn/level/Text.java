package id.shadowyn.level;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class Text {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();

    private Text() {
    }

    public static Component c(String text) {
        return LEGACY.deserialize(text == null ? "" : text);
    }

    public static Component item(String text) {
        return c("&r" + (text == null ? "" : text)).decoration(TextDecoration.ITALIC, false);
    }

    public static String s(String text) {
        if (text == null) return "";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(?i)&?#([0-9a-f]{6})").matcher(text);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder legacy = new StringBuilder("\u00A7x");
            for (int i = 0; i < hex.length(); i++) legacy.append('\u00A7').append(hex.charAt(i));
            matcher.appendReplacement(builder, java.util.regex.Matcher.quoteReplacement(legacy.toString()));
        }
        matcher.appendTail(builder);
        return builder.toString().replace('&', '\u00A7');
    }
}
