package com.kotdex.utils.checkers.handlers

import com.kotdex.utils.checkers.IChecker

public interface ICheckerHandler<P, R> {
    public fun addChecker(checker: IChecker<P>): ICheckerHandler<P, R>

    public fun handle(data: P, returnOnFirstError: Boolean = false): R
}
