package de.knutwalker.dbpedia.util

import java.util.Locale
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

class NamedThreadFactory private (threadNamePrefix: String) extends ThreadFactory {

  private[this] final val group = Option(System.getSecurityManager).fold(Thread.currentThread().getThreadGroup)(_.getThreadGroup)
  private[this] final val threadNumber = new AtomicInteger(1)

  def newThread(r: Runnable): Thread = {
    val t = new Thread(group, r, s"$threadNamePrefix-${threadNumber.getAndIncrement}", 0)
    t.setDaemon(false)
    t.setPriority(Thread.NORM_PRIORITY)
    t
  }
}

object NamedThreadFactory {
  private[this] final val DEFAULT_NAME = "DBpedia"
  private[this] final val NAME_PATTERN = "%s-%d-thread"
  private[this] final val threadPoolNumber = new AtomicInteger(1)

  def apply(): NamedThreadFactory = apply(DEFAULT_NAME)

  def apply(namePrefix: String): NamedThreadFactory = {
    val checkedPrefix = Option(namePrefix).filter(_.nonEmpty).getOrElse(DEFAULT_NAME)
    val prefix = NAME_PATTERN.formatLocal(Locale.ROOT, checkedPrefix, threadPoolNumber.getAndIncrement)
    new NamedThreadFactory(prefix)
  }
}
