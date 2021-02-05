package net.machinemuse.anima
package campfire

import entity.EntityLightSpirit
import registration.RegistryHelpers._

import net.minecraft.block.{BlockState, Blocks}
import net.minecraft.entity.SpawnReason
import net.minecraft.item.DyeColor
import net.minecraft.nbt.CompoundNBT
import net.minecraft.tileentity.{CampfireTileEntity, TileEntityType}
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.vector.Vector3d
import net.minecraft.util.text.TranslationTextComponent
import net.minecraftforge.api.distmarker.{Dist, OnlyIn}
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent
import org.apache.logging.log4j.scala.Logging

import scala.annotation.nowarn
import scala.jdk.CollectionConverters.ListHasAsScala
import scala.util.Random

/**
 * Created by MachineMuse on 1/24/2021.
 */
object CampfirePlusTileEntity {
  @SubscribeEvent def onConstructMod(e: FMLConstructModEvent) = {}

  val CAMPFIREPLUS_TE = regTE[CampfirePlusTileEntity]("campfireplus", () => new CampfirePlusTileEntity, () => CampfirePlus.getBlock)
  def getType = CAMPFIREPLUS_TE.get()
}

@Mod.EventBusSubscriber(modid = Anima.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
class CampfirePlusTileEntity extends CampfireTileEntity with Logging {
  def copyOldTE(blockstate: BlockState, oldTE: CampfireTileEntity): Unit = {
    val oldNBT: CompoundNBT = oldTE.write(new CompoundNBT)
    this.read(blockstate, oldNBT)
  }

  var colour1: Int = DyeColor.GREEN.getTextColor
  var colour2: Int = DyeColor.LIME.getTextColor

  var dance_enhancement: Double = 0.0F

  val DANCE_RANGE = 50

  @OnlyIn(Dist.CLIENT)
  override def getRenderBoundingBox: AxisAlignedBB = {
    val bb = new AxisAlignedBB(pos.add(-1, 0, -1), pos.add(2, 2 + dance_enhancement/500, 2))
    bb
  }

  override def tick(): Unit = {
    val nearbyPlayers = world.getPlayers.asScala.toList.flatMap {
      case player if player.getDistanceSq(Vector3d.copy(getPos)) < DANCE_RANGE * DANCE_RANGE => Some(player)
      case _ => None
    }
    dance_enhancement = 0
    nearbyPlayers.foreach { player =>
      val danceScore = DanceTracker.getPlayerDanceScore(player)
      if(danceScore > 1000) {
        dance_enhancement += danceScore - 1000
      }
    }

    if(Random.nextInt(500) == 0) {
      logger.debug("random tick from Campfire Plus")
      world.onServer{ serverWorld =>
        val randX = Random.between(-50, 50)
        val randY = Random.between(-10, 10)
        val randZ = Random.between(-50, 50)
        val blockPlace = pos.add(randX, randY, randZ)
        val spawnLocationState = world.getBlockState(blockPlace)
        if(spawnLocationState.isAir(world, blockPlace) && spawnLocationState.getBlock != Blocks.VOID_AIR : @nowarn ) {
          val newEnt = EntityLightSpirit.getType.spawn(serverWorld, null, new TranslationTextComponent("lightspirit"), null, blockPlace, SpawnReason.SPAWNER, true, true)
          if(newEnt != null) {
            newEnt.homeblock.set(blockPlace)
            newEnt.attention.set(Random.between(10.minutesInTicks, 30.minutesInTicks))
          }
          logger.debug("new entity " + newEnt + " created")
        }
      }
    }
    super.tick()
  }

  override def getType: TileEntityType[CampfirePlusTileEntity] = CampfirePlusTileEntity.getType

  override def read(blockstate : BlockState, compound : CompoundNBT): Unit = {
    super.read(blockstate, compound)
    if(compound.contains("colour1")) {
      colour1 = compound.getInt("colour1")
    }
    if(compound.contains("colour2")) {
      colour2 = compound.getInt("colour2")
    }
  }

  override def write(compound : CompoundNBT): CompoundNBT = {
    compound.putInt("colour1", colour1)
    compound.putInt("colour2", colour2)
    super.write(compound)
  }

  override def getUpdateTag: CompoundNBT = {
    val items = super.getUpdateTag
    items.putInt("colour1", colour1)
    items.putInt("colour2", colour2)
    items
  }
}
