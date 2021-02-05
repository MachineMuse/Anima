package net.machinemuse.anima
package util

import org.apache.logging.log4j.scala.Logging

/**
 * Created by MachineMuse on 1/29/2021.
 */
object NBTTypeRef extends Logging {
  @inline final val END: Byte = 0
  @inline final val BYTE: Byte = 1
  @inline final val SHORT: Byte = 2
  @inline final val INT: Byte = 3
  @inline final val LONG: Byte = 4
  @inline final val FLOAT: Byte = 5
  @inline final val DOUBLE: Byte = 6
  @inline final val BYTE_ARRAY: Byte = 7
  @inline final val STRING: Byte = 8
  @inline final val TAG_LIST: Byte = 9
  @inline final val TAG_COMPOUND: Byte = 10
  @inline final val INT_ARRAY: Byte = 11
  @inline final val LONG_ARRAY: Byte = 12
}
