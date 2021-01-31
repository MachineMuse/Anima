package net.machinemuse

import net.minecraft.world.World
import net.minecraft.world.server.ServerWorld
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.eventbus.api.Event
import org.apache.logging.log4j.scala.Logger

import java.util.function.Consumer

/**
 * Created by MachineMuse on 1/29/2021.
 */
package object anima {
  implicit class Optionable[A](a: A) {
    def some = Some(a)
    def none = None
  }

  implicit class RichNumeric[T](a: T)(implicit nt: Numeric[T]) {
    import nt._
    final def secondsInTicks = a * nt.fromInt(20)
    final def minutesInTicks = a * nt.fromInt(20) * nt.fromInt(60)
    def isFromUntil(b: T, c: T) = (a >= b && a < c) ||
                                    (a < b && a >= c)

  }

  implicit class FoldableID[A](optA: Option[A]) {
    def foldId[B](func: (B, A) => B): B => B = {
      optA.fold(identity[B] _) { a => func(_, a) }
    }
  }

  implicit class AndDoable[T](ret: T) {
    def andDo(f: T => ()): T = {
      f(ret)
      ret
    }
    def andLog(f: Logger => Unit)(implicit logr: Logger): T = {
      f(logr)
      ret
    }
  }

  implicit class ButFirstAble0[O](f: () => O) {
    final def butFirst(g: () => Unit): () => O = {
      () =>
        g()
        f()
    }
  }

  implicit class ButFirstAble1[A, O](f: A => O) {
    final def butFirst(g: () => Unit): A => O = {
      a: A =>
        g()
        f(a)
    }
  }
  implicit class ButFirstAble2[A, B, O](f: (A, B) => O) {
    final def butFirst(g: () => Unit): (A, B) => O = {
      (a: A, b: B) =>
        g()
        f(a,b)
    }
  }
  implicit class ButFirstAble3[A, B, C, O](f: (A, B, C) => O) {
    final def butFirst(g: () => Unit): (A, B, C) => O = {
      (a: A, b: B, c: C) =>
        g()
        f(a,b,c)
    }
  }

  // Nullsafe!
  object OptionCast {
    def apply[T: Manifest](x: Any): Option[T] =
      if (implicitly[Manifest[T]].runtimeClass.isInstance(x)) {
        Some(x.asInstanceOf[T])
      } else {
        None
      }
  }

  implicit class Optionally[I](x: I) {
    def optionallyAs[O: Manifest]: Option[O] =
      if (implicitly[Manifest[O]].runtimeClass.isInstance(x)) {
        Some(x.asInstanceOf[O])
      } else {
        None
      }

    //    def optionallyAsList[O: Manifest]: List[O] =
    //      if (implicitly[Manifest[O]].runtimeClass.isInstance(x)) {
    //        List(x.asInstanceOf[O])
    //      } else {
    //        List.empty[O]
    //      }

    def optionallyDoAs[O: Manifest](f: O => ()): Unit =
      if (implicitly[Manifest[O]].runtimeClass.isInstance(x)) {
        f(x.asInstanceOf[O])
      } else {
        ()
      }

    def doAsOrElse[O: Manifest, D](default: D)(f: O => D): D = {
      if(implicitly[Manifest[O]].runtimeClass.isInstance(x)) {
        f(x.asInstanceOf[O])
      } else {
        default
      }
    }
  }

  implicit class OptionallyIterable[A] (list: Iterable[A]) {
    def iterateAs[O: Manifest]: Iterable[O] = {
      list.flatMap{ a =>
        if(implicitly[Manifest[O]].runtimeClass.isInstance(a)) {
          Iterable(a.asInstanceOf[O])
        } else {
          Iterable.empty
        }
      }
    }
  }
  implicit class WorldOptionals(world: World) {
    def onServer(f: ServerWorld => Unit): Unit = {
      if(!world.isRemote) {
        world.optionallyDoAs[ServerWorld] {sw =>
          f(sw)
        }
      }
    }
  }

  def addForgeListeners(listeners: Consumer[_ <: Event]*) = for(listener <- listeners) {MinecraftForge.EVENT_BUS.addListener(listener)}
}
