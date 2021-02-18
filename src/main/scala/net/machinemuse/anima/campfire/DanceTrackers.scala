package net.machinemuse.anima
package campfire

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.vector.Vector3d
import net.minecraftforge.event.entity.living.LivingEvent.{LivingJumpEvent, LivingUpdateEvent}
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import org.apache.logging.log4j.scala.Logging

import scala.collection.mutable

/**
 * Created by MachineMuse on 2/4/2021.
 */
@EventBusSubscriber(modid = Anima.MODID, bus = Bus.FORGE)
object DanceTrackers extends Logging {
  // TODO: Optimize the heck out of this, it's very expensive atm!!!

  private val TIMEOUT = 1000 // Milliseconds

  private val entityMap = new mutable.HashMap[java.util.UUID, DanceTracker]

  class DanceTracker {
    val lookQueue = SummingTimedMutableQueueWithLast[Vector3d, Double] (0) {
      case (sum, current, Some(last)) => sum + Math.sqrt(current.subtract(last).length)
      case (sum, current, None) => 0 // This is the first node, nothing to compare to
    } {
      case (sum, current, Some(next)) => sum - Math.sqrt(next.subtract(current).length)
      case (sum, current, None) => 0 // No remaining nodes after dequeueing this one
    }
    val velocityQueue = SummingTimedMutableQueueWithLast[Vector3d, Vector3d] (Vector3d.ZERO) {
      case (sum, current, Some(last)) => sum.add(current.subtract(last))
      case (sum, current, None) => Vector3d.ZERO // This is the first node, nothing to compare to
    } {
      case (sum, current, Some(next)) => sum.subtract(next.subtract(current))
      case (sum, current, None) => Vector3d.ZERO // No remaining nodes after dequeueing this one
    }
    val velocityMagQueue = SummingTimedMutableQueueWithLast[Vector3d, Double] (0) {
      case (sum, current, Some(last)) => sum + current.subtract(last).length()
      case (sum, current, None) => 0 // This is the first node, nothing to compare to
    } {
      case (sum, current, Some(next)) => sum - next.subtract(current).length()
      case (sum, current, None) => 0 // No remaining nodes after dequeueing this one
    }
    val accelQueue = SummingTimedMutableQueueWithLast[Vector3d, Vector3d] (Vector3d.ZERO) {
      case (sum, current, Some(last)) => sum.add(current.subtract(last))
      case (sum, current, None) => Vector3d.ZERO // This is the first node, nothing to compare to
    } {
      case (sum, current, Some(next)) => sum.subtract(next.subtract(current))
      case (sum, current, None) => Vector3d.ZERO // No remaining nodes after dequeueing this one
    }
    val accelMagQueue = SummingTimedMutableQueueWithLast[Vector3d, Double] (0) {
      case (sum, current, Some(last)) => sum + current.subtract(last).length()
      case (sum, current, None) => 0 // This is the first node, nothing to compare to
    } {
      case (sum, current, Some(next)) => sum - next.subtract(current).length()
      case (sum, current, None) => 0 // No remaining nodes after dequeueing this one
    }
    val sneakingQueue = SummingTimedMutableQueue.uncheckedBooleanQueue
    val jumpingQueue = SummingTimedMutableQueue.numericQueue[Int]
    val danceValueQueue = SummingTimedMutableQueue.numericQueue[Double]
    var wasSneaking = false

    def update(player: PlayerEntity) = {
      val now = System.currentTimeMillis()
      lookQueue.add(now, player.getLookVec)
      lookQueue.dropTo(now - TIMEOUT)

      val lastPos = velocityQueue.peekLatest
      val horzPos = player.getPositionVec.mul(1.0, 0.0, 1.0)
      velocityQueue.add(now, horzPos)
      velocityMagQueue.add(now, horzPos)
      velocityQueue.dropTo(now - TIMEOUT)
      velocityMagQueue.dropTo(now - TIMEOUT)

      lastPos.foreach { oldPos =>
        val vel = horzPos.subtract(oldPos)
        accelQueue.add(now, vel)
        accelMagQueue.add(now, vel)
      }
      accelQueue.dropTo(now - TIMEOUT)
      accelMagQueue.dropTo(now - TIMEOUT)

      if(wasSneaking != player.isSneaking) {
        sneakingQueue.add(now, true)
        wasSneaking = player.isSneaking
      }
      sneakingQueue.dropTo(now - TIMEOUT)

      danceValueQueue.add(now, getDanceValue(player))
      danceValueQueue.dropTo(now - TIMEOUT * 5)
    }
    def jump(): Unit = {
      jumpingQueue.add(System.currentTimeMillis(), 1)
    }

    def getDanceValue(player: PlayerEntity): Double = {
      val lookAveragePerSecond = lookQueue.getSum * 20 / lookQueue.size
      val look = Math.log(lookAveragePerSecond * 4 + 1) * 2

      val accelScore = accelMagQueue.getSum * (TIMEOUT / 50) / accelMagQueue.size - accelQueue.getSum.length()
      val speedScore = velocityMagQueue.getSum * (TIMEOUT / 50) / velocityMagQueue.size - velocityQueue.getSum.length()
      val motion = accelScore * speedScore * 10

      val sneak = Math.log(sneakingQueue.getSum + 1) * 5

      val jump = Math.log(jumpingQueue.getSum + 1) * 8

      val danceScore = look + motion + sneak + jump

      danceScore
    }
  }

  object SummingTimedMutableQueue {
    def numericQueue[T](implicit num: Numeric[T]) = {
      import num._
      new SimpleSummingTimedMutableQueue[T, T](num.zero, _+_, _-_)
    }
    def vectorQueue = new SimpleSummingTimedMutableQueue[Vector3d, Vector3d](Vector3d.ZERO, _.add(_), _.subtract(_))
    def uncheckedBooleanQueue = new SimpleSummingTimedMutableQueue[Boolean, Int](0, (i, b) => i + 1, (i, b) => i - 1)
  }


  trait SummingTimedMutableQueue[T, S] {
    def zero: S
    def plus: (S, T) => S
    def minus: (S, T) => S

    protected case class Node(time: Long, el: T, var next: Option[Node])
    protected var LATEST = none[Node]
    protected var OLDEST = none[Node]
    protected var SUM = zero
    protected var SIZE = 0

    def size = SIZE

    def peekLatest = LATEST.map(_.el)

    def getSum = SUM

    def add(timestamp: Long, el: T): Unit = {
      val newNode = Node(timestamp, el, none).some
      SUM = plus(SUM, el)
      LATEST = LATEST.fold(newNode) { oldLatest =>
        oldLatest.next = newNode
        newNode
      }
      if(OLDEST.isEmpty) {
        OLDEST = LATEST
      }
      SIZE += 1
    }

    def dropTo(deadline: Long): Unit = {
      while(OLDEST.exists(_.time < deadline)) {
        val oldestNode = OLDEST.get
        SUM = minus(SUM, oldestNode.el)
        OLDEST = oldestNode.next
        SIZE -= 1
      }
    }
  }

  case class SimpleSummingTimedMutableQueue[T, S](zero: S, plus: (S, T) => S, minus: (S, T) => S) extends SummingTimedMutableQueue[T, S]

  case class SummingTimedMutableQueueWithLast[T, S](zero: S)
                                                   (plusWithLast: (S, T, Option[T]) => S)
                                                   (minusWithNext: (S, T, Option[T]) => S) extends SummingTimedMutableQueue[T, S] {
    override def plus: (S, T) => S = (sum, el) => plusWithLast(sum, el, LATEST.map(_.el))

    override def minus: (S, T) => S = (sum, el) => minusWithNext(sum, el, OLDEST.flatMap(_.next.map(_.el)))
  }

  @SubscribeEvent def onUpdate(event: LivingUpdateEvent) = {
    if(!event.getEntityLiving.world.isRemote) {

      event.getEntityLiving.optionallyDoAs[PlayerEntity] { player =>
        val data = entityMap.getOrElseUpdate(player.getUniqueID, new DanceTracker)
        data.update(player)
      }
    }
  }


  def getPlayerDanceData(player: PlayerEntity) = {
    entityMap.getOrElseUpdate(player.getUniqueID, new DanceTracker())
  }
  def getPlayerDanceScore(player: PlayerEntity) = {
    val data = getPlayerDanceData(player)
    val aggDanceScore = data.danceValueQueue.getSum
    aggDanceScore
  }

  @SubscribeEvent def onJump(event: LivingJumpEvent): Unit = {
    event.getEntityLiving.optionallyDoAs[PlayerEntity] { player =>
      getPlayerDanceData(player).jump()
    }
  }
}
