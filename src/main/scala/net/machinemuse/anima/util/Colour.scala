package net.machinemuse.anima
package util

/**
 * Created by MachineMuse on 1/27/2021.
 */
object Colour {

  @inline final def redFromInt(value: Int): Float = (value >> 16 & 255).toFloat / 255.0F
  @inline final def greenFromInt(value: Int): Float = (value >> 8 & 255).toFloat / 255.0F
  @inline final def blueFromInt(value: Int): Float = (value & 255).toFloat / 255.0F

  @inline final def colourFromFloats(r: Float, g: Float, b: Float): Int = (r * 255).toInt * 65536 + (g * 255).toInt * 256 + (b * 255).toInt

  @inline final def mixColoursByRatio(c1: Int, c2: Int, ratio: Float = 1.0F): Int = {
    val denominator = ratio + 1.0F
    val newred = (redFromInt(c1) * ratio + redFromInt(c2)) / denominator
    val newgreen = (greenFromInt(c1) * ratio + greenFromInt(c2) ) / denominator
    val newblue = (blueFromInt(c1) * ratio + blueFromInt(c2)) / denominator
    colourFromFloats(newred, newgreen, newblue)
  }

  @inline final def mixColoursByWeight(c1: Int, c2: Int, w1: Float, w2: Float): Int = {
    val denominator = w1+w2
    val newred   = (  redFromInt(c1) * w1 +   redFromInt(c2) * w2) / denominator
    val newgreen = (greenFromInt(c1) * w1 + greenFromInt(c2) * w2) / denominator
    val newblue  = ( blueFromInt(c1) * w1 +  blueFromInt(c2) * w2) / denominator
    colourFromFloats(newred, newgreen, newblue)
  }

  @inline final def toTuple(value: Int): (Float, Float, Float) = (redFromInt(value), greenFromInt(value), blueFromInt(value))
  @inline final def toFloatArray(value: Int): Array[Float] = Array(redFromInt(value), greenFromInt(value), blueFromInt(value))
  @inline final def fromFloatArray(values: Array[Float]): Int = colourFromFloats(values(0), values(1), values(2))

}
