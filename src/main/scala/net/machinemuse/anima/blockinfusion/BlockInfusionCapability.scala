package net.machinemuse.anima
package blockinfusion

import net.minecraft.block._
import net.minecraft.tags.FluidTags
import net.minecraft.util.math.{BlockPos, ChunkPos}
import net.minecraft.world._
import net.minecraft.world.chunk.Chunk
import net.minecraft.world.server.ServerWorld
import net.minecraftforge.common.IPlantable
import net.minecraftforge.common.capabilities.{Capability, CapabilityInject}
import net.minecraftforge.event.AttachCapabilitiesEvent
import net.minecraftforge.event.TickEvent.WorldTickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import org.apache.logging.log4j.scala.Logging

import scala.collection.{concurrent, mutable}
import scala.jdk.CollectionConverters.IterableHasAsScala
import scala.ref.WeakReference

import blockinfusion.BlockInfusionCapability.BlockInfusionInterface
import blockinfusion.BlockInfusionEvents.SoilElement
import registration.RegistryHelpers.{mkCapabilityProviderWithSaveData, regCapWithStorage}
import util.GenCodecsByName._
import util.VanillaCodecs._

/**
 * Created by MachineMuse on 2/20/2021.
 */
object BlockInfusionCapability extends Logging {
  private val ACTIVE_INTERFACES = concurrent.TrieMap.empty[WeakReference[World], mutable.Map[WeakReference[Chunk], BlockInfusionInterface]]

  val INFUSABLE_BLOCKS = Set(
    Blocks.DIRT,
    Blocks.COARSE_DIRT,
    Blocks.FARMLAND,
    Blocks.WATER,
    Blocks.LAVA,
    Blocks.SAND,
    Blocks.SOUL_SAND,
    Blocks.CRIMSON_NYLIUM,
    Blocks.WARPED_NYLIUM,
    Blocks.MYCELIUM,
    Blocks.PODZOL,
    Blocks.GRASS_BLOCK)

  @SubscribeEvent def attachCapabilities(event: AttachCapabilitiesEvent[Chunk]) = {
    val newCap = new BlockInfusionCapability
    val worldRefOpt = ACTIVE_INTERFACES.keySet.find(_.get.contains(event.getObject.getWorld))
    val (worldInterfaces, worldRef): (mutable.Map[WeakReference[Chunk], BlockInfusionInterface], WeakReference[World]) = worldRefOpt match {
      case Some(worldRef) =>
        (ACTIVE_INTERFACES(worldRef), worldRef)
      case None =>
        (concurrent.TrieMap.empty, WeakReference(event.getObject.getWorld))
    }
    worldInterfaces.update(WeakReference(event.getObject), newCap)
    ACTIVE_INTERFACES.update(worldRef, worldInterfaces)
    event.addCapability(
      modLoc("block_infusion_capability"),
      mkCapabilityProviderWithSaveData[BlockInfusionInterface](getCapability, () => newCap)
    )
  }

  val ADJACENT_SEQ = Seq(0, -1, 1)

  def blockPosToChunkLong(x: Int, z: Int) = ChunkPos.asLong(x >> 4, z >> 4)

  @SubscribeEvent def onWorldTick(event: WorldTickEvent): Unit = {
    event.world.onServer { world =>
      val profiler = event.world.getProfiler
      profiler.startSection("Ticking elemental infusion")
      val worldRefOpt = ACTIVE_INTERFACES.keySet.find(_.get.contains(world))
      for {
        worldRef <- worldRefOpt
        chunks <- ACTIVE_INTERFACES.get(worldRef)
        chunkRef <- chunks.keySet.iterator
      } {
        chunkRef.get match {
          case None => chunks.remove(chunkRef)
          case Some(chunk) =>
            if (world.getChunkProvider.isChunkLoaded(chunk.getPos)) {
              chunks.get(chunkRef) match {
                case Some(cap) =>
                  profiler.startSection("Generating chunks for random ticks")

                  val nearbyChunkPositions = {for {
                      _ <- Map.empty[Long, Chunk]
                      x <- ADJACENT_SEQ
                      z <- ADJACENT_SEQ
                      nearbyPos = new ChunkPos(chunk.getPos.x + x, chunk.getPos.z + z)
                      nearbyChunk <- Option( world.getChunkProvider.getChunk(nearbyPos.x, nearbyPos.z, false))
                    } yield (nearbyPos.asLong, nearbyChunk)}

                  profiler.endStartSection(s"Doing ${world.getGameRules.getInt(GameRules.RANDOM_TICK_SPEED)} random ticks")

                  cap.randomTick(chunk, world, nearbyChunkPositions)
                  profiler.endSection()
                case _ =>
              }
            }
        }

      }
      profiler.endSection()
    }
  }

  type BlockInfusionData = mutable.Map[BlockPos, mutable.Map[SoilElement.Value, Int]]

  trait BlockInfusionInterface extends SavedData[BlockInfusionData] {

    def getDataForPos(pos: BlockPos): mutable.Map[SoilElement.Value, Int]

    def getElementAtPos(pos: BlockPos, elem: SoilElement.Value): Int

    def setDataForPos(pos: BlockPos, values: mutable.Map[SoilElement.Value, Int]): Unit

    def setElementAtPos(pos: BlockPos, elem: SoilElement.Value, value: Int): Unit

    def randomTick(chunk: Chunk, world: ServerWorld, cache: Map[Long, Chunk]): Unit

    def removeDataForPos(pos: BlockPos): Unit
  }

  @CapabilityInject(classOf[BlockInfusionInterface])
  private var CAPABILITY: Capability[BlockInfusionInterface] = null
  def getCapability = Option(CAPABILITY).getOrElse {
    logger.error("Block Infusion capability injection failed")
    ???
  }

  regCapWithStorage[BlockInfusionData, BlockInfusionInterface](() => new BlockInfusionCapability)
}

@EventBusSubscriber(modid = Anima.MODID, bus = Bus.FORGE)
class BlockInfusionCapability extends BlockInfusionInterface with Logging {
  import BlockInfusionCapability._

  private val blockData: BlockInfusionData = mutable.HashMap.empty[BlockPos, mutable.Map[SoilElement.Value, Int]]

  override def getData: BlockInfusionData = blockData

  override def getDataForPos(pos: BlockPos): mutable.Map[SoilElement.Value, Int] = {
    blockData.getOrElse(pos, mutable.HashMap.empty)
  }

  override def setDataForPos(pos: BlockPos, values: mutable.Map[SoilElement.Value, Int]): Unit = {
    if(values.isEmpty) {
      blockData.remove(pos)
    } else {
      blockData.put(pos, values)
    }
  }

  override def removeDataForPos(pos: BlockPos): Unit = {
    blockData.remove(pos)
  }

  override def getElementAtPos(pos: BlockPos, elem: SoilElement.Value): Int = getDataForPos(pos).getOrElse(elem, 0)

  override def setElementAtPos(pos: BlockPos, elem: SoilElement.Value, value: Int): Unit = {
    val innerData = getDataForPos(pos)
    if(value == 0) {
      innerData.remove(elem)
    } else {
      innerData.update(elem, value)
    }
    setDataForPos(pos, innerData)
  }

  override def putData(data: BlockInfusionData): Unit = {
    blockData.clear()
    for{
      (ko, vo) <- data
      (ki, vi) <- vo
    } {
      setElementAtPos(ko, ki, vi)
    }
  }

  private def isValidToBeAbove(state: BlockState): Boolean = {
    if(!state.isSolid)
      true
    else
      false
  }

  override def randomTick(chunk: Chunk, world : ServerWorld, nearbyChunkPositions: Map[Long, Chunk]): Unit = {
    for {section <- chunk.getSections.filter(_ != Chunk.EMPTY_SECTION)
         i <- 0 until world.getGameRules.getInt(GameRules.RANDOM_TICK_SPEED)
         } {
      val xOrigin = chunk.getPos.getXStart
      val yOrigin = section.getYLocation
      val zOrigin = chunk.getPos.getZStart
      val origin = world.getBlockRandomPos(xOrigin, yOrigin, zOrigin, 15)
      val blockAtOrigin = section.getBlockState(origin.getX - xOrigin, origin.getY - yOrigin, origin.getZ - zOrigin)
      val blockAbove = chunk.getBlockState(origin.up())
      if(INFUSABLE_BLOCKS.contains(blockAtOrigin.getBlock) && isValidToBeAbove(blockAbove)) {
        for {
          pos <- BlockPos.getAllInBoxMutable(origin.add(-1, 1, -1), origin.add(1, 1, 1)).asScala
          chunk <- nearbyChunkPositions.get(blockPosToChunkLong(pos.getX, pos.getZ))
          plant = chunk.getBlockState(pos).getBlock if plant.isInstanceOf[IPlantable]
          behaviour <- BlockInfusionEvents.BEHAVIOURS.get(plant)
          infuses <- behaviour.infuses
        } {
          val value = getElementAtPos(origin, infuses)
          if (value < 3) {
            setElementAtPos(origin, infuses, value + 1)
          }
        }
        //      profiler.startSection("Iterating through positions")
        for {
          pos <- BlockPos.getAllInBoxMutable(origin.add(-4, 0, -4), origin.add(4, 1, 4)).asScala
          chunk <- nearbyChunkPositions.get(blockPosToChunkLong(pos.getX, pos.getZ))
          checkState = chunk.getFluidState(pos) if !checkState.isEmpty
        } {
          //        profiler.endStartSection("Checking positions")
          if(checkState.isTagged(FluidTags.WATER)) {
            val value = getElementAtPos(origin, SoilElement.Water)
            if (value < 3) {
              setElementAtPos(origin, SoilElement.Water, value + 1)
            }
          } else if(checkState.isTagged(FluidTags.LAVA)) {
            val value = getElementAtPos(origin, SoilElement.Fire)
            if (value < 3) {
              setElementAtPos(origin, SoilElement.Fire, value + 1)
            }
          }
          //        profiler.endStartSection("Iterating through positions")
        }
      } else {
        removeDataForPos(origin)
      }
      //      profiler.endSection()
    }
  }


}