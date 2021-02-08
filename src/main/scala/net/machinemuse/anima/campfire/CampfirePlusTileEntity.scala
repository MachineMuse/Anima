package net.machinemuse.anima
package campfire

import campfire.CampfirePlusTileEntity.{DustInfo, dustCodec}
import entity.EntityLightSpirit
import registration.RegistryHelpers._
import util.NBTTypeRef
import util.VanillaCodecs.ConvenientCodec

import com.mojang.serialization.Codec
import net.minecraft.block.{BlockState, Blocks}
import net.minecraft.entity.{EntityType, SpawnReason}
import net.minecraft.item.{DyeColor, ItemStack}
import net.minecraft.nbt.CompoundNBT
import net.minecraft.tileentity.{CampfireTileEntity, TileEntityType}
import net.minecraft.util.math._
import net.minecraft.util.math.vector.Vector3d
import net.minecraft.world.LightType
import net.minecraft.world.gen.Heightmap
import net.minecraft.world.server.ServerWorld
import net.minecraftforge.api.distmarker.{Dist, OnlyIn}
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent
import org.apache.logging.log4j.scala.Logging

import scala.annotation.{nowarn, tailrec}
import scala.jdk.CollectionConverters.ListHasAsScala
import scala.util.Random

/**
 * Created by MachineMuse on 1/24/2021.
 */
object CampfirePlusTileEntity {
  @SubscribeEvent def onConstructMod(e: FMLConstructModEvent) = {}

  val CAMPFIREPLUS_TE = regTE[CampfirePlusTileEntity]("campfireplus", () => new CampfirePlusTileEntity, () => CampfirePlus.getBlock)
  def getType = CAMPFIREPLUS_TE.get()

  /*_*/
  import util.VanillaCodecs._
  private val dustCodec = implicitly[Codec[List[DustInfo]]]
  /*_*/

  case class DustInfo(outerColour: Int, innerColour: Int, attracts: List[EntityType[_]])

  val defaultOuterColour: Int = DyeColor.RED.getColorValue
  val defaultInnerColour: Int = DyeColor.YELLOW.getTextColor

  def dustInfoFromItemStack(stack: ItemStack): Option[DustInfo] = {
    if(stack.hasTag) {
      val attracts = if(stack.getTag.contains("attracts")) {
        val listNBT = stack.getTag.getList("attracts", NBTTypeRef.TAG_COMPOUND)
        val attractsList = implicitly[Codec[List[EntityType[_]]]].parseINBT(listNBT)
        attractsList.getOrElse(List.empty)
      } else List.empty
      val colour1 = if(stack.getTag.contains("colour1")) stack.getTag.getInt("colour1") else defaultOuterColour
      val colour2 = if(stack.getTag.contains("colour2")) stack.getTag.getInt("colour2") else defaultInnerColour

      DustInfo(colour1, colour2, attracts).some
    } else {
      None
    }
  }
}

@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
class CampfirePlusTileEntity extends CampfireTileEntity with Logging {
  def copyOldTE(blockstate: BlockState, oldTE: CampfireTileEntity): Unit = {
    val oldNBT: CompoundNBT = oldTE.write(new CompoundNBT)
    this.read(blockstate, oldNBT)
  }


  var dance_enhancement: Double = 0.0F

  var activeDusts: List[DustInfo] = List.empty
  val DANCE_RANGE = 50

  @OnlyIn(Dist.CLIENT)
  override def getRenderBoundingBox: AxisAlignedBB = {
    val bb = new AxisAlignedBB(pos.add(-1, 0, -1), pos.add(2, 2 + dance_enhancement/500, 2))
    bb
  }

  def trySpawnEntity(serverWorld: ServerWorld, tries: Int, entityType: EntityType[_]): Unit = {
    logger.debug("Trying to spawn a Light Spirit")
    val randDir = Random.between(0.0, Math.PI*2)
    val randLen = Random.between(0.0, 50.0)
    val x = randLen * Math.sin(randDir) + pos.getX
    val z = randLen * Math.cos(randDir) + pos.getZ
    val y = serverWorld.getHeight(Heightmap.Type.WORLD_SURFACE, x.toInt, z.toInt)
    val blockPlace = new BlockPos(x, y, z)
    if(serverWorld.getChunkProvider.isChunkLoaded(new ChunkPos(blockPlace))){
      val deeper = goDeeper(serverWorld, blockPlace, blockPlace)
      if(serverWorld.getLightFor(LightType.BLOCK, deeper) < 8) {
        checkBlockAndSpawn(serverWorld, deeper, tries, entityType)
      } else if (tries > 0) {
        logger.trace(s"Light level too high at $deeper; trying a different spot")
        trySpawnEntity(serverWorld, tries-1, entityType)
      } else {
        logger.info(s"Failed attempt to spawn light spirit; couldn't find a good spawn location")
      }
    }

  }

  @tailrec
  final def checkBlockAndSpawn(world: ServerWorld, blockPlace: BlockPos, tries: Int, entityType: EntityType[_]): Unit = {
    val blockState = world.getBlockState(blockPlace)
    if(blockState.isAir && blockState.getBlock != Blocks.VOID_AIR : @nowarn) {
      spawnEntity(world, blockPlace, entityType)
    } else if(tries > 0) {
      logger.trace(s"Spawn location invalid at $blockPlace; trying one higher")
      checkBlockAndSpawn(world, blockPlace.up(), tries-1, entityType)
    }
  }


  @tailrec
  final def goDeeper(world: ServerWorld, blockPlaceStart: BlockPos, foundBlock: BlockPos): BlockPos = {
    val deeperPos = blockPlaceStart.down()
    val blockStateHere = world.getBlockState(blockPlaceStart)
    val blockStateDeeper = world.getBlockState(deeperPos)
    val newFoundBlock = if(blockStateHere.isAir : @nowarn) blockPlaceStart else foundBlock
    if(blockStateDeeper.isSolid) {
      logger.trace(s"Went deeper from $blockPlaceStart, found $foundBlock")
      newFoundBlock
    } else {
      goDeeper(world, deeperPos, newFoundBlock)
    }
  }

  def spawnEntity(serverWorld: ServerWorld, blockPlace: BlockPos, entityType: EntityType[_]): Unit = {
    val newEnt = entityType.spawn(serverWorld, null, null, null, blockPlace, SpawnReason.SPAWNER, true, true)
    newEnt.optionallyDoAs[EntityLightSpirit] { lightspirit =>
      lightspirit.homeblock.set(blockPlace)
      lightspirit.attention.set(Random.between(10.minutesInTicks, 30.minutesInTicks))

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

      if(dance_enhancement > 0 && activeDusts.nonEmpty) {
        for{dust <- activeDusts
            entityType <- dust.attracts
            } {
          if(Random.nextInt(800 * 10) < dance_enhancement) {
            trySpawnEntity(serverWorld, 5, entityType)
          }
        }
      }
    }
    super.tick()
  }

  override def getType: TileEntityType[CampfirePlusTileEntity] = CampfirePlusTileEntity.getType



  override def read(blockstate : BlockState, compound : CompoundNBT): Unit = {
    super.read(blockstate, compound)
    if(compound.contains("dusts")) {
      val dustsNBT = compound.getList("dusts", NBTTypeRef.TAG_COMPOUND)
      val dustsOpt = dustCodec.parseINBT(dustsNBT)
      dustsOpt.fold[Unit]({activeDusts = List.empty})(dusts => activeDusts = dusts)
    }
    if(compound.contains("dance_enhancement")) {
      dance_enhancement = compound.getFloat("dance_enhancement")
    }
  }

  override def write(compound : CompoundNBT): CompoundNBT = {
    compound.putFloat("dance_enhancement", dance_enhancement.toFloat)
    val dustsNBT = dustCodec.writeINBT(activeDusts)
    compound.put("dusts", dustsNBT)
    super.write(compound)
  }

  override def getUpdateTag: CompoundNBT = {
    val items = super.getUpdateTag
    val dustsNBT = dustCodec.writeINBT(activeDusts)
    items.put("dusts", dustsNBT)
    items.putFloat("dance_enhancement", dance_enhancement.toFloat)
    items
  }

  override def handleUpdateTag(blockstate: BlockState, compound: CompoundNBT): Unit = {
    super.handleUpdateTag(blockstate, compound)
    if(compound.contains("dusts")) {
      val dustsNBT = compound.getList("dusts", NBTTypeRef.TAG_COMPOUND)
      val dustsOpt = dustCodec.parseINBT(dustsNBT)
      dustsOpt.fold[Unit]({activeDusts = List.empty})(dusts => activeDusts = dusts)
    }
    if(compound.contains("dance_enhancement")) {
      dance_enhancement = compound.getFloat("dance_enhancement")
    }
  }
}
