package com.kotdex.utils.checkers

public interface IChecker<P> {
    public fun check(data: P): FailedCheck?
}
