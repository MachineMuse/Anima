package net.machinemuse.anima
package campfire

import entity.EntityLightSpirit
import registration.RegistryHelpers._

import net.minecraft.block.{BlockState, Blocks}
import net.minecraft.entity.SpawnReason
import net.minecraft.item.DyeColor
import net.minecraft.nbt.CompoundNBT
import net.minecraft.tileentity.{CampfireTileEntity, TileEntityType}
import net.minecraft.util.math.vector.Vector3d
import net.minecraft.util.math._
import net.minecraft.util.text.TranslationTextComponent
import net.minecraft.world.gen.Heightmap
import net.minecraft.world.server.ServerWorld
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

  def trySpawnLightSpirit(serverWorld: ServerWorld): Unit = {
    logger.info("Trying to spawn a Light Spirit")
    val randDir = Random.between(0.0, Math.PI*2)
    val randLen = Random.between(0.0, 50.0)
    val x = randLen * Math.sin(randDir) + pos.getX
    val z = randLen * Math.cos(randDir) + pos.getZ
    val y = serverWorld.getHeight(Heightmap.Type.WORLD_SURFACE, x.toInt, z.toInt)
    val blockPlace = new BlockPos(x, y, z)
    if(serverWorld.getChunkProvider.isChunkLoaded(new ChunkPos(blockPlace))) {
      var success = false
      var yAdd = 0
      while(yAdd < 5 && !success) {
        val newPos = blockPlace.add(0, yAdd, 0)
        val newState = serverWorld.getBlockState(newPos)
        if (newState.isAir(serverWorld, newPos) && newState.getBlock != Blocks.VOID_AIR: @nowarn) {
          spawnLightSpirit(serverWorld, newPos)
          success = true
        } else {
          yAdd += 1
        }

      }
    }

  }
  def spawnLightSpirit(serverWorld: ServerWorld, blockPlace: BlockPos): Unit = {
    val newEnt = EntityLightSpirit.getType.spawn(serverWorld, null, new TranslationTextComponent("lightspirit"), null, blockPlace, SpawnReason.SPAWNER, true, true)
    if (newEnt != null) {
      newEnt.homeblock.set(blockPlace)
      newEnt.attention.set(Random.between(10.minutesInTicks, 30.minutesInTicks))
    }
    logger.info("new entity " + newEnt + " created")
  }

  override def tick(): Unit = {
    world.onServer { serverWorld =>

      val nearbyPlayers = world.getPlayers.asScala.toList.flatMap {
        case player if player.getDistanceSq(Vector3d.copy(getPos)) < DANCE_RANGE * DANCE_RANGE => Some(player)
        case _ => None
      }
      dance_enhancement = 0
      nearbyPlayers.foreach { player =>
        val danceScore = DanceTracker.getPlayerDanceScore(player)
        dance_enhancement += MathHelper.clamp(danceScore - 800, 0, 800)
//        if (Random.nextInt(20) == 0) logger.info("Dance Thing: " + dance_enhancement)

//        world.setBlockState(this.getPos, this.getBlockState, BlockStateFlags.STANDARD_CLIENT_UPDATE)
        //        this.markDirty()
      }
      serverWorld.getChunkProvider().markBlockChanged(this.getPos) // send update to clients

      if(dance_enhancement > 0) {
        if(Random.nextInt(800 * 10) < dance_enhancement) {
          trySpawnLightSpirit(serverWorld)
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
    if(compound.contains("dance_enhancement")) {
      dance_enhancement = compound.getFloat("dance_enhancement")
    }
  }

  override def write(compound : CompoundNBT): CompoundNBT = {
    compound.putInt("colour1", colour1)
    compound.putInt("colour2", colour2)
    compound.putFloat("dance_enhancement", dance_enhancement.toFloat)
    super.write(compound)
  }

  override def getUpdateTag: CompoundNBT = {
    val items = super.getUpdateTag
    items.putInt("colour1", colour1)
    items.putInt("colour2", colour2)
    items.putFloat("dance_enhancement", dance_enhancement.toFloat)
    items
  }

  override def handleUpdateTag(blockstate: BlockState, compound: CompoundNBT): Unit = {
    super.handleUpdateTag(blockstate, compound)
    if(compound.contains("colour1")) {
      colour1 = compound.getInt("colour1")
    }
    if(compound.contains("colour2")) {
      colour2 = compound.getInt("colour2")
    }
    if(compound.contains("dance_enhancement")) {
      dance_enhancement = compound.getFloat("dance_enhancement")
    }
  }
}
