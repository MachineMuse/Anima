package net.machinemuse.anima.util

import com.mojang.blaze3d.matrix.MatrixStack
import org.apache.logging.log4j.LogManager

/**
 * Created by MachineMuse on 1/28/2021.
 */
object ScalaShorthand {
  private val LOGGER = LogManager.getLogger


  implicit class FoldableID[A](optA: Option[A]) {
    def foldId[B](func: (B, A) => B): B => B = {
      optA.fold(identity[B] _) { a => func(_, a) }
    }
  }

  implicit class AndDoable[T](ret: T) {
    def andDo(f: T => ()) = {
      f(ret)
      ret
    }
  }

  implicit class ButFirstAble1[A, B](f: A => B) {
    final def butFirst(g: () => Unit): A => B = {
      a: A =>
      g()
      f(a)
    }
  }
  implicit class ButFirstAble2[A, B, C](f: (A, B) => C) {
    final def butFirst(g: () => Unit): (A, B) => C = {
      (a: A, b: B) =>
        g()
        f(a,b)
    }
  }

  def withPushedMatrix(m: MatrixStack) (f: MatrixStack.Entry => Unit) = {
    m.push()
    f(m.getLast)
    m.pop()
  }

}
