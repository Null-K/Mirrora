package com.puddingkc.Mirrora.util;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.ArrayList;
import java.util.List;


public final class Messages {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String ACCENT_COLOR = "#83c2eb";
    private static final String PREFIX = "<" + ACCENT_COLOR + ">Mirrora <#cee2f0>|</#cee2f0> ";

    public enum Level {
        INFO("<#a4adb3>"),
        SUCCESS("<#a4adb3>"),
        WARN("<#a4adb3>"),
        ERROR("<#a4adb3>");

        private final String color;

        Level(String color) {
            this.color = color;
        }
    }

    private Messages() { }

    public static Component format(Level level, String message, Object... args) {
        String raw = PREFIX + level.color + message;
        return MINI_MESSAGE.deserialize(raw, resolvers(args));
    }

    private static TagResolver resolvers(Object[] args) {
        List<TagResolver> resolvers = new ArrayList<>(args.length + 1);
        for (int i = 0; i < args.length; i++) {
            String value = String.valueOf(args[i]);
            resolvers.add(Placeholder.unparsed("arg" + (i + 1), value));
            if (i == 0) {
                resolvers.add(Placeholder.unparsed("arg", value));
            }
        }
        return TagResolver.resolver(resolvers);
    }

    public static Component info(String message, Object... args) {
        return format(Level.INFO, message, args);
    }

    public static Component success(String message, Object... args) {
        return format(Level.SUCCESS, message, args);
    }

    public static Component warn(String message, Object... args) {
        return format(Level.WARN, message, args);
    }

    public static Component error(String message, Object... args) {
        return format(Level.ERROR, message, args);
    }

    public static void send(Audience audience, Level level, String message, Object... args) {
        audience.sendMessage(format(level, message, args));
    }

    public static void info(Audience audience, String message, Object... args) {
        audience.sendMessage(info(message, args));
    }

    public static void success(Audience audience, String message, Object... args) {
        audience.sendMessage(success(message, args));
    }

    public static void warn(Audience audience, String message, Object... args) {
        audience.sendMessage(warn(message, args));
    }

    public static void error(Audience audience, String message, Object... args) {
        audience.sendMessage(error(message, args));
    }
}
