package net.machinemuse.anima
package catstatue

import net.minecraft.entity.CreatureEntity
import net.minecraft.entity.ai.RandomPositionGenerator
import net.minecraft.entity.ai.goal.Goal
import net.minecraft.entity.monster.CreeperEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.vector.Vector3d
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.event.entity.living.LivingSpawnEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import org.apache.logging.log4j.scala.Logging

import java.util

/**
 * Created by MachineMuse on 2/12/2021.
 */
object AvoidScaryBlocksGoal extends Logging {
  @SubscribeEvent def onSpawnMob(event: LivingSpawnEvent.SpecialSpawn): Unit = {
    event.getEntityLiving.optionallyDoAs[CreeperEntity] { creeper =>
      creeper.goalSelector.addGoal(3,
        new AvoidScaryBlocksGoal(creeper, 16, 1.0D, 1.2D)
      )
    }
  }
  @SubscribeEvent def onJoinWorld(event: EntityJoinWorldEvent): Unit = {
    event.getEntity.optionallyDoAs[CreeperEntity] { creeper =>
      creeper.goalSelector.addGoal(3,
        new AvoidScaryBlocksGoal(creeper, 16, 1.0D, 1.2D)
      )
    }
  }
}
@EventBusSubscriber(modid = Anima.MODID, bus = Bus.FORGE)
class AvoidScaryBlocksGoal(val entity: CreatureEntity,
                           val avoidDistance: Float,
                           val farSpeed: Double,
                           val nearSpeed: Double) extends Goal with Logging {

  val avoidDistancesq = avoidDistance * avoidDistance
  val pathNavigator = entity.getNavigator
  setMutexFlags(util.EnumSet.of(Goal.Flag.MOVE))

  private var blockScaredOf: Option[BlockPos] = None
  var path: Option[net.minecraft.pathfinding.Path] = None

  @inline private final def toChunkCoord(i: Double) = Math.floor(i/16).toInt
  private def getNearbyScaryBlocks = {
    val lowerxbound = toChunkCoord(entity.getPosX - avoidDistance)
    val lowerybound = toChunkCoord(entity.getPosY - avoidDistance)
    val lowerzbound = toChunkCoord(entity.getPosZ - avoidDistance)
    val upperxbound = toChunkCoord(entity.getPosX + avoidDistance)
    val upperybound = toChunkCoord(entity.getPosY + avoidDistance)
    val upperzbound = toChunkCoord(entity.getPosZ + avoidDistance)
    val catsListofLists = for {
      chunkX <- lowerxbound to upperxbound
      chunkZ <- lowerzbound to upperzbound
    } yield {
      val chunk = entity.world.getChunk(chunkX, chunkZ)
      val cap = chunk.getCapability(CatStatueTrackingCapability.getCapability).resolve().get()
      val cats = cap.getData.filter(pos =>
        entity.getDistanceSq(pos.getX, pos.getY, pos.getZ) < avoidDistancesq
      )
      cats
    }
    catsListofLists.flatten
  }

  override def shouldExecute(): Boolean = {
    blockScaredOf = getNearbyScaryBlocks.sortBy(pos => entity.getDistanceSq(Vector3d.copy(pos))).headOption
    blockScaredOf.fold(false) { pos =>
      val catPosD = Vector3d.copy(pos)
      val fleeTo = RandomPositionGenerator.findRandomTargetBlockAwayFrom(this.entity, 16, 7, catPosD)
      if(fleeTo == null) {
        false
      } else if (fleeTo.squareDistanceTo(catPosD) < entity.getDistanceSq(catPosD)) {
        false
      } else {
        path = Option(pathNavigator.getPathToPos(fleeTo.x, fleeTo.y, fleeTo.z, 0))
        path.nonEmpty
      }
    }
  }

  override def shouldContinueExecuting(): Boolean = !pathNavigator.noPath()

  override def startExecuting(): Unit =
    pathNavigator.setPath(path.orNull, farSpeed)

  override def resetTask(): Unit = blockScaredOf = None

  override def tick(): Unit =
    blockScaredOf.foreach { pos =>
      val distancesq = entity.getDistanceSq(pos.getX, pos.getY, pos.getZ)
      distancesq match {
        case _ if distancesq < avoidDistancesq / 4 => pathNavigator.setSpeed(nearSpeed)
        case _ if distancesq < avoidDistancesq => pathNavigator.setSpeed(farSpeed)
        case _ =>
      }
    }
}
