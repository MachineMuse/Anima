package net.machinemuse.anima
package util

import org.apache.logging.log4j.LogManager

/**
 * Created by MachineMuse on 2/21/2021.
 */
trait Logging {
  lazy val logger = LogManager.getLogger(getClass.getName)
}
