package com.l1sk1sh.vladikbot.models;

/**
 * @author Oliver Johnson
 */
@FunctionalInterface
public interface SettingsFunction<T> {
    @SuppressWarnings("unused")
    void set(T t);
}