package com.kotdex.builders

public interface IBuilder<out R> {
    public fun build(): R
}
