package com.kotdex.scanned

import com.kotdex.annotations.Discord
import com.kotdex.annotations.Slash

@Discord
class ScannedClass {
    @Slash("scanned", "Found by scanning")
    fun scanned() {}
}

@Discord
object ScannedObject {
    @Slash("singleton", "Found by scanning")
    fun singleton() {}
}

class NotAnnotated {
    @Slash("hidden", "Not scanned")
    fun hidden() {}
}
