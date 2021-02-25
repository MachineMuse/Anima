package net.machinemuse.anima
package blockinfusion

import net.minecraft.block._
import net.minecraft.tags.FluidTags
import net.minecraft.util.math.{BlockPos, ChunkPos}
import net.minecraft.world._
import net.minecraft.world.chunk.Chunk
import net.minecraft.world.server.ServerWorld
import net.minecraftforge.common.capabilities.{Capability, CapabilityInject}
import net.minecraftforge.event.AttachCapabilitiesEvent
import net.minecraftforge.event.TickEvent.WorldTickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus

import scala.annotation.tailrec
import scala.collection.mutable
import scala.jdk.CollectionConverters.IterableHasAsScala

import blockinfusion.BlockInfusionCapability.BlockInfusionInterface
import blockinfusion.BlockInfusionEvents.SoilElement
import registration.RegistryHelpers.{mkCapabilityProviderWithSaveData, regCapWithStorage}
import util.ChunkDataHandler.{WorldData, WorldLoadedChunkDataHandler}
import util.GenCodecsByName._
import util.Logging
import util.VanillaCodecs._

/**
 * Created by MachineMuse on 2/20/2021.
 */
object BlockInfusionCapability extends Logging {

  private val LOADED_CHUNK_REFS = new WorldLoadedChunkDataHandler[BlockInfusionInterface]

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
    LOADED_CHUNK_REFS.putData(event.getObject.getWorld, event.getObject, newCap)
    event.addCapability(
      modLoc("block_infusion_capability"),
      mkCapabilityProviderWithSaveData[BlockInfusionInterface](getCapability, () => newCap)
    )
  }

  val ADJACENT_SEQ = Seq(0, -1, 1)
  val ADJACENT_MAP = {for {
    x <- ADJACENT_SEQ
    z <- ADJACENT_SEQ
  } yield (x, z) -> ()}.toMap

  def blockPosToChunkLong(x: Int, z: Int) = ChunkPos.asLong(x >> 4, z >> 4)

  @SubscribeEvent def onWorldTick(event: WorldTickEvent): Unit = {
    event.world.onServer { world =>
      val profiler = world.getProfiler
      profiler.startSection("Cleaning Chunk Refs")
//      LOADED_CHUNK_REFS.clean(world)
      profiler.endStartSection("Ticking elemental infusion")
      for {
        worldData <- LOADED_CHUNK_REFS.getData(world)
        (chunk, cap) <- worldData.iterator
      } {
//        profiler.startSection("Generating chunks for random ticks")
//        val nearbyChunkPositions = {
//          for {
//            ((x, z), _) <- ADJACENT_MAP
//            nearbyPos = ChunkPos.asLong(chunk.getPos.x + x, chunk.getPos.z + z)
//            (nearbyChunkRef, cap) <- worldData.contents.get(nearbyPos)
//            nearbyChunk <- nearbyChunkRef.get
//          } yield nearbyPos -> (nearbyChunk, cap)
//        }
//        profiler.endStartSection(s"Doing ${world.getGameRules.getInt(GameRules.RANDOM_TICK_SPEED)} random ticks")
        cap.randomTick(chunk, world, worldData)
//        profiler.endSection()
      }
      profiler.endSection()
    }
  }

  type BlockInfusionData = mutable.Map[BlockPos, mutable.Map[SoilElement.Value, Int]]

  trait BlockInfusionInterface extends SavedData[BlockInfusionData] {
    def increaseElementAtPos(pos: BlockPos, elem: SoilElement.Value, increase: Int, max: Int): Int

    def decreaseElementAtPos(pos: BlockPos, elem: SoilElement.Value, decrease: Int, min: Int): Int

    def getDataForPos(pos: BlockPos): mutable.Map[SoilElement.Value, Int]

    def getElementAtPos(pos: BlockPos, elem: SoilElement.Value): Int

    def setDataForPos(pos: BlockPos, values: mutable.Map[SoilElement.Value, Int]): Unit

    def setElementAtPos(pos: BlockPos, elem: SoilElement.Value, value: Int): Unit

    def randomTick(chunk: Chunk, world: ServerWorld, cache: WorldData[BlockInfusionInterface]): Unit

    def removeDataForPos(pos: BlockPos): Unit
  }

  @CapabilityInject(classOf[BlockInfusionInterface])
  private var CAPABILITY: Capability[BlockInfusionInterface] = null
  def getCapability = Option(CAPABILITY).getOrElse {
    logger.error("Block Infusion capability injection failed")
    ???
  }

  implicit class ChunkWithCapability(chunk: Chunk) {
    def getAndRealizeCapability[T](cap: Capability[T]) = chunk.getCapability(cap).orElseThrow{
      () => new Exception(s"Failed gathering capability $cap for chunk $chunk")
    }
  }

  regCapWithStorage[BlockInfusionData, BlockInfusionInterface](() => new BlockInfusionCapability)
}

@EventBusSubscriber(modid = Anima.MODID, bus = Bus.FORGE)
class BlockInfusionCapability extends BlockInfusionInterface {
  import BlockInfusionCapability._

  private val blockData: BlockInfusionData = mutable.HashMap.empty[BlockPos, mutable.Map[SoilElement.Value, Int]]

  override def getData: BlockInfusionData = blockData

  // returns the actual change in value after limiting by max (can be negative if max is lower than existing)
  override def increaseElementAtPos(pos: BlockPos, elem: BlockInfusionEvents.SoilElement.Value, increase: Int, max: Int): Int = {
    val prev = getElementAtPos(pos, elem)
    val next = Math.min(prev + increase, max)
    setElementAtPos(pos, elem, next)
    next - prev
  }

  // returns the actual change in value after limiting by min (can be negative if min is higher than existing)
  override def decreaseElementAtPos(pos: BlockPos, elem: BlockInfusionEvents.SoilElement.Value, decrease: Int, min: Int): Int = {
    val prev = getElementAtPos(pos, elem)
    val next = Math.max(prev - decrease, min)
    setElementAtPos(pos, elem, next)
    prev - next
  }

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


  implicit class BlockWithInteraction(blockState: BlockState) {
    final def isInfuser: Boolean = {
      BlockInfusionEvents.BEHAVIOURS.contains(blockState.getBlock)
    }
    final def isInfusable: Boolean = {
      INFUSABLE_BLOCKS.contains(blockState.getBlock)
    }
    final def getInfusionBehaviour = {
      BlockInfusionEvents.BEHAVIOURS.get(blockState.getBlock)
    }
    final def isValidToBeAbove: Boolean = {
      if(!blockState.isSolid)
        true
      else
        false
    }
  }

  @tailrec
  private def getBlocksBelowPlant(pos: BlockPos, world: IWorld): Iterable[BlockPos] = {
    if(!world.getBlockState(pos.down()).isInfuser) {
      BlockPos.getAllInBoxMutable(pos.add(-1, -1, -1), pos.add(1, -1, 1)).asScala
    } else {
      getBlocksBelowPlant(pos.down(), world)
    }
  }

  override def randomTick(originChunk: Chunk, world : ServerWorld, worldData: WorldData[BlockInfusionInterface]): Unit = {
    for {section <- originChunk.getSections if section != Chunk.EMPTY_SECTION
         i <- 0 until world.getGameRules.getInt(GameRules.RANDOM_TICK_SPEED)
         } {
      val xOrigin = originChunk.getPos.getXStart
      val yOrigin = section.getYLocation
      val zOrigin = originChunk.getPos.getZStart
      val origin = world.getBlockRandomPos(xOrigin, yOrigin, zOrigin, 15)
      val blockAtOrigin = section.getBlockState(origin.getX - xOrigin, origin.getY - yOrigin, origin.getZ - zOrigin)
      val behaviourAtOrigin = blockAtOrigin.getInfusionBehaviour
      if(behaviourAtOrigin.isDefined) {
        val behaviour = behaviourAtOrigin.get
        for {consumes <- behaviour.consumes} {
          val blocksUnder = for {
            underBlockPos <- getBlocksBelowPlant(origin, world)
            (chunk, cap) <- worldData.getOrCache(underBlockPos, originChunk, this)
            underBlockState = chunk.getBlockState(underBlockPos) if underBlockState.isInfusable
          } yield (underBlockPos, cap)
          if(blocksUnder.nonEmpty) {
            val (bestPos, bestCap) = blocksUnder.maxBy { case (pos, cap) => cap.getElementAtPos(pos, consumes) }
            val bestValue = bestCap.getElementAtPos(bestPos, consumes)
            if (bestValue > 0) {
              bestCap.decreaseElementAtPos(bestPos, consumes, 1, 0)
              increaseElementAtPos(origin, consumes, 1, 9)
            }
          }
        }
      } else if(blockAtOrigin.isInfusable && originChunk.getBlockState(origin.up).isValidToBeAbove) {
        for {
          pos <- BlockPos.getAllInBoxMutable(origin.add(-1, 1, -1), origin.add(1, 1, 1)).asScala
        } {
          worldData.getOrCache(pos, originChunk, this).foreach { case (chunk, cap) =>
            chunk.getBlockState(pos).getInfusionBehaviour.foreach(behaviour =>
              behaviour.infuses.foreach(infuses =>
                increaseElementAtPos(origin, infuses, 1, 3)
              )
            )
          }
        }
        for (
          pos <- BlockPos.getAllInBoxMutable(origin.add(-4, 0, -4), origin.add(4, 1, 4)).asScala
        ) {
          worldData.getOrCache(pos, originChunk, this).foreach { case (chunk, cap) =>
            val checkState = chunk.getFluidState(pos)
            if(checkState.isEmpty) {
              // Nothing?
            } else if(checkState.isTagged(FluidTags.WATER)) {
              increaseElementAtPos(origin, SoilElement.Water, 1, 3)
            } else if(checkState.isTagged(FluidTags.LAVA)) {
              increaseElementAtPos(origin, SoilElement.Fire, 1, 3)
            }
          }
        }
      } else {
        removeDataForPos(origin)
      }
    }
  }


}