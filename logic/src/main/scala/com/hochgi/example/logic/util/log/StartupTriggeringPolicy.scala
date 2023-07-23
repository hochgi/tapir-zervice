package com.hochgi.example.logic.util.log

import ch.qos.logback.core.joran.spi.NoAutoStart
import ch.qos.logback.core.rolling.TriggeringPolicyBase

import java.io.File

@NoAutoStart
class StartupTriggeringPolicy[E] extends TriggeringPolicyBase[E] {

  private[this] var firstTimeInvocationAlreadyOccurred = false

  override def isTriggeringEvent(activeFile: File, event: E): Boolean =
    if (firstTimeInvocationAlreadyOccurred) false else {
      firstTimeInvocationAlreadyOccurred = true
      true
    }
}
