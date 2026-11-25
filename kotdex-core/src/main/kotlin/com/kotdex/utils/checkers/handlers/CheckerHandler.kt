package com.kotdex.utils.checkers.handlers

import com.kotdex.utils.checkers.FailedCheck
import com.kotdex.utils.checkers.IChecker

public typealias CheckerHandlerResult = List<FailedCheck?>

public class CheckerHandler<P> : ICheckerHandler<P, CheckerHandlerResult> {
    private var checkers: MutableList<IChecker<P>?> = mutableListOf()

    override fun addChecker(checker: IChecker<P>): ICheckerHandler<P, CheckerHandlerResult> {
        checkers.add(checker)

        return this
    }

    override fun handle(data: P, returnOnFirstError: Boolean): CheckerHandlerResult {
        if (checkers.isEmpty()) return emptyList()

        val errors: MutableList<FailedCheck?> = mutableListOf()

        checkers.forEach { checker ->
            val failed = checker?.check(data)
            if (failed == null) return@forEach
            if (returnOnFirstError) return listOf(failed)

            errors.add(failed)
        }

        return errors
    }
}
