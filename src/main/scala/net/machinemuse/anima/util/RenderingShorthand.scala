package net.machinemuse.anima
package util

import com.mojang.blaze3d.matrix.MatrixStack

/**
 * Created by MachineMuse on 1/28/2021.
 */
object RenderingShorthand {

  def withPushedMatrix(m: MatrixStack) (f: MatrixStack.Entry => Unit) = {
    m.push()
    f(m.getLast)
    m.pop()
  }

}
