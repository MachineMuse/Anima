package net.machinemuse

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt._
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math._
import net.minecraft.world.World
import net.minecraft.world.server.ServerWorld
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.util.NonNullSupplier
import net.minecraftforge.eventbus.api.Event
import net.minecraftforge.fml.event.lifecycle.ParallelDispatchEvent

import java.util.concurrent.Callable
import java.util.function.Consumer

/**
 * Created by MachineMuse on 1/29/2021.
 */
package object anima {

  case class MODID(name: String) {
    override def toString = name
  }
  implicit def modidToString(m: MODID) = m.name
  implicit final val ANIMA_MODID = MODID(Anima.MODID)

  def modLoc(path: String) = new ResourceLocation(implicitly[MODID], path)

  implicit class Optionable[A](a: A) {
    def some: Option[A] = Some(a)
    def none: Option[A] = None
  }
  def none[A]: Option[A] = None

  trait NBTTagAccessor[T] {
    def getFromCompound(tag: CompoundNBT, name: String): T
    def putInCompound(tag: CompoundNBT, name: String, item: T): Unit

    def getFromList(tag: ListNBT, index: Int): T
    def putInList(tag: ListNBT, index: Int, item: T): Unit
  }

  implicit class RichCompoundNBT(compound: CompoundNBT) {
    def putT[T](name: String, item: T)(implicit accessor: NBTTagAccessor[T]) = accessor.putInCompound(compound, name, item)
    def getT[T](name: String)(implicit accessor: NBTTagAccessor[T]): T = accessor.getFromCompound(compound, name)
  }

  implicit object IntNBTTagAccessor extends NBTTagAccessor[Int] {
    override def getFromCompound(tag: CompoundNBT, name: String): Int = tag.getInt(name)
    override def putInCompound(tag: CompoundNBT, name: String, item: Int): Unit = tag.putInt(name, item)

    override def getFromList(tag: ListNBT, index: Int): Int = tag.getInt(index)
    override def putInList(tag: ListNBT, index: Int, item: Int): Unit = tag.add(index, IntNBT.valueOf(item))
  }


  implicit class RichTuple2[A, B](t: (A, B)) {
    def mapFirst[C](f: A => C) = (f(t._1), t._2)
    def mapSecond[C](f: B => C) = (t._1, f(t._2))
  }
  implicit class RichNumeric[T](a: T)(implicit nt: Numeric[T]) {
    import nt._
    final def secondsInTicks = a * nt.fromInt(20)
    final def minutesInTicks = a * nt.fromInt(20) * nt.fromInt(60)
    def isFromUntil(b: T, c: T) = (a >= b && a < c) ||
                                    (a < b && a >= c)
    def isFromTo(b: T, c: T) = (a >= b && a <= c) ||
      (a <= b && a >= c)
  }

  implicit class FoldableID[A](optA: Option[A]) {
    def foldId[B](func: (B, A) => B): B => B = {
      optA.fold(identity[B] _) { a => func(_, a) }
    }
  }

  implicit class AndDoable[T](ret: T) {
    def andDo(f: T =>  Unit): T = {
      f(ret)
      ret
    }
//    def andLog(f: Logger => Unit)(implicit logr: Logger): T = {
//      f(logr)
//      ret
//    }
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

  // TODO: replace with ClassTag and stuff

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
    final def optionallyAs[O: Manifest]: Option[O] =
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

    final def optionallyDoAs[O: Manifest](f: O => Unit): Unit =
      if (implicitly[Manifest[O]].runtimeClass.isInstance(x)) {
        f(x.asInstanceOf[O])
      } else {
        ()
      }

    final def optionallyDoAsIfNot[O: Manifest, S: Manifest](f: O => Unit): Unit =
      if (implicitly[Manifest[O]].runtimeClass.isInstance(x) && !implicitly[Manifest[S]].runtimeClass.isInstance(x)) {
        f(x.asInstanceOf[O])
      } else {
        ()
      }

    final def mapAsOrElse[O: Manifest, D](default: D)(f: O => D): D = {
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
    def onClient(f: World => Unit): Unit = {
      if(world.isRemote) {
        f(world)
      }
    }
    def onEach[T](serverDo: ServerWorld => T) (clientDo: World => T) (default: T): T = {
      if(!world.isRemote) {
        world.mapAsOrElse[ServerWorld, T](default) { sw =>
          serverDo(sw)
        }
      } else {
        clientDo(world)
      }
    }
  }

  def rayTrace(worldIn: World, player: PlayerEntity, fluidMode: RayTraceContext.FluidMode): BlockRayTraceResult = {
    val pitch = player.rotationPitch
    val yaw = player.rotationYaw
    val near = player.getEyePosition(1.0F)
    val yawx = MathHelper.cos(-yaw * (Math.PI.toFloat / 180F) - Math.PI.toFloat)
    val yawz = MathHelper.sin(-yaw * (Math.PI.toFloat / 180F) - Math.PI.toFloat)
    val pitchxz = -MathHelper.cos(-pitch * (Math.PI.toFloat / 180F))
    val looky = MathHelper.sin(-pitch * (Math.PI.toFloat / 180F))
    val lookx = yawz * pitchxz
    val lookz = yawx * pitchxz
    val reach = player.getAttribute(net.minecraftforge.common.ForgeMod.REACH_DISTANCE.get).getValue

    val far = near.add(lookx.toDouble * reach, looky.toDouble * reach, lookz.toDouble * reach)
    worldIn.rayTraceBlocks(new RayTraceContext(near, far, RayTraceContext.BlockMode.OUTLINE, fluidMode, player))
  }

  object JavaFunctionConverters {
    import java.util.function._
    implicit def consumer[A] (f: A => Unit): Consumer[A] = f(_)
    implicit def biconsumer[A, B](f: (A, B) => Unit): BiConsumer[A, B] = f(_,_)

    implicit def function1[A, B](f: A => B): Function[A, B] = f(_)
    implicit def bifunction[A, B, O](f: (A, B) => O): BiFunction[A, B, O] = f(_,_)

    implicit def supplier[A] (f: () => A): Supplier[A] = () => f()

    implicit def nonNullSupplier[A] (f: () => A): NonNullSupplier[A] = () => f()

    implicit def suppliercallable[A] (f: () => () => A): Supplier[Callable[A]] = () => () => f()()

    implicit def callable[A] (f: () => A): Callable[A] = () => f()

    implicit def biconsumer1P2Supplier[P,S](f: (P, () => S) => Unit): BiConsumer[P, Supplier[S]] = (p, s) => f(p,() => s.get())
  }

  implicit def MCUnitToScalaUnit(unit : Unit): net.minecraft.util.Unit = net.minecraft.util.Unit.INSTANCE
  implicit def ScalaUnitToMCUnit(unit: net.minecraft.util.Unit): Unit = ()

  implicit class OffThreadEvent(evt: ParallelDispatchEvent) {
    def doOnMainThread(f: () => Unit) = evt.enqueueWork(new Runnable() {def run() = f()})
  }
  def addForgeListeners(listeners: Consumer[_ <: Event]*) = for(listener <- listeners) {MinecraftForge.EVENT_BUS.addListener(listener)}
}
