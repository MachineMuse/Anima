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
object DanceTracker extends Logging {
  // TODO: Optimize the heck out of this, it's very expensive atm!!!

  private val entityMap = new mutable.HashMap[java.util.UUID, DanceData]

  class DanceData(
                   val lookQueue: mutable.Queue[VectorEntry],
                   val posQueue: mutable.Queue[VectorEntry],
                   val sneakingQueue: mutable.Queue[BooleanEntry],
                   val jumpingQueue: mutable.Queue[UnitEntry],
                   val danceValueQueue: mutable.Queue[DoubleEntry]
                  ) {

  }
  private def newDanceData() = new DanceData(mutable.Queue.empty,mutable.Queue.empty,mutable.Queue.empty,mutable.Queue.empty,mutable.Queue.empty)

  trait QueueEntry {
    def timestamp: Long
  }
  case class VectorEntry(timestamp: Long, vector: Vector3d) extends QueueEntry
  case class BooleanEntry(timestamp: Long, boolean: Boolean) extends QueueEntry
  case class UnitEntry(timestamp: Long) extends QueueEntry
  case class DoubleEntry(timestamp: Long, double: Double) extends QueueEntry


  private val TIMEOUT = 1000

  @SubscribeEvent def onUpdate(event: LivingUpdateEvent) = {
    if(!event.getEntityLiving.world.isRemote) {

      event.getEntityLiving.optionallyDoAs[PlayerEntity] { player =>
        val now = System.currentTimeMillis()
        val data = entityMap.getOrElseUpdate(player.getUniqueID, newDanceData())
        data.lookQueue.enqueue(VectorEntry(now, player.getLookVec))
        data.lookQueue.dequeueWhile(_.timestamp < now - TIMEOUT)
        val entPos = player.getPositionVec.mul(1.0, 0.0, 1.0)
        data.posQueue.enqueue(VectorEntry(now, entPos))
        data.posQueue.dequeueWhile(_.timestamp < now - TIMEOUT)
        data.sneakingQueue.enqueue(BooleanEntry(now, player.isSneaking))
        data.sneakingQueue.dequeueWhile(_.timestamp < now - TIMEOUT)

        data.danceValueQueue.enqueue(DoubleEntry(now, getDanceValue(player)))
        data.danceValueQueue.dequeueWhile(_.timestamp < now - TIMEOUT * 5)
      }
    }
  }


  def getDanceValue(player: PlayerEntity): Double = {
    val data = entityMap.getOrElseUpdate(player.getUniqueID, newDanceData())

    val lookValueIterator = data.lookQueue.sliding(2).toSeq.map {
      case mutable.Queue(a,b) => Math.sqrt(a.vector.subtract(b.vector).length())
      case _ => 0.0
    }
    val lookValue = lookValueIterator.sum * (TIMEOUT / 50) / lookValueIterator.size
    val look = Math.log(lookValue * 4 + 1) * 2

    val velocities: Seq[Vector3d] = data.posQueue.sliding(2).map {
      case mutable.Queue(a,b) => b.vector.subtract(a.vector)
      case _ => Vector3d.ZERO
    }.toSeq

    val accelerations: Seq[Vector3d] = velocities.sliding(2).map {
      case Seq(a,b) => b.subtract(a)
      case _ => Vector3d.ZERO
    }.toSeq

    val accelerationsMagnitude: Seq[Double] = accelerations.map (_.length)

    val accelerationsSum = accelerations.foldLeft(Vector3d.ZERO) {_.add(_)}

    val speeds: Seq[Double] = velocities.map { velocity =>
      velocity.length
    }
    val velocitySum: Vector3d = velocities.foldLeft(Vector3d.ZERO) {_.add(_)}

    val accelScore = accelerationsMagnitude.sum * (TIMEOUT / 50) / accelerationsMagnitude.size - accelerationsSum.length
    val speedScore = speeds.sum * (TIMEOUT / 50) / speeds.size - velocitySum.length

    val motion = accelScore * speedScore * 10

    val sneakValue = data.sneakingQueue.sliding(2).map {
      case mutable.Queue(a,b) => if(a.boolean != b.boolean) 1 else 0
      case _ => 0
    }.sum
    val sneak = Math.log(sneakValue + 1) * 5

    data.jumpingQueue.dequeueWhile(_.timestamp < System.currentTimeMillis() - TIMEOUT)
    val jumpValue = data.jumpingQueue.map(_ => 1).sum
    val jump = Math.log(jumpValue + 1) * 8

    val danceScore = look + motion + sneak + jump

//    if(Random.nextInt(10) == 0) {
//      logger.info(f"Look: [$look%.3f] | Motion: [$motion%.3f] | Sneak: [$sneak%.3f] | Jump: [$jump%.3f] | Score: [$danceScore%.3f]")
//      val aggDanceScore = data.danceValueQueue.map(_.double).sum
//      logger.info(f"Dance Score (5sec): $aggDanceScore%.3f")
//    }

    danceScore
  }

  def getPlayerDanceData(player: PlayerEntity) = {
    entityMap.getOrElseUpdate(player.getUniqueID, newDanceData())
  }
  def getPlayerDanceScore(player: PlayerEntity) = {
    val data = getPlayerDanceData(player)
    val aggDanceScore = data.danceValueQueue.map(_.double).sum
    aggDanceScore
  }

  @SubscribeEvent def onJump(event: LivingJumpEvent): Unit = {
    event.getEntityLiving.optionallyDoAs[PlayerEntity] { player =>
      val now = System.currentTimeMillis()
      val data = entityMap.getOrElseUpdate(player.getUniqueID, newDanceData())
      data.jumpingQueue.addOne(UnitEntry(now))
      data.jumpingQueue.dequeueWhile(_.timestamp < now - TIMEOUT)
    }
  }
}
