package net.machinemuse.anima.util

import org.apache.logging.log4j.scala.Logging

/**
 * Created by MachineMuse on 1/29/2021.
 */
object NBTTypeRef extends Logging {
  val END: Byte = 0
  val BYTE: Byte = 1
  val SHORT: Byte = 2
  val INT: Byte = 3
  val LONG: Byte = 4
  val FLOAT: Byte = 5
  val DOUBLE: Byte = 6
  val BYTE_ARRAY: Byte = 7
  val STRING: Byte = 8
  val TAG_LIST: Byte = 9
  val TAG_COMPOUND: Byte = 10
  val INT_ARRAY: Byte = 11
  val LONG_ARRAY: Byte = 12
}
