package net.machinemuse.anima
package blockinfusion

import net.minecraft.block._
import net.minecraft.tags.FluidTags
import net.minecraft.util.math.{BlockPos, ChunkPos}
import net.minecraft.world._
import net.minecraft.world.chunk.{Chunk, ChunkSection}
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
import blockinfusion.BlockInfusionEvents.{ElementalBehaviour, SoilElement}
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
      profiler.startSection("Ticking elemental infusion")
      for {
        worldData <- LOADED_CHUNK_REFS.getData(world)
        (chunk, cap) <- worldData.iterator
      } {
        cap.randomTick(chunk, world, worldData)
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
    def removeDataForPos(pos: BlockPos): Option[ElementalBehaviour] // Returns none
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
  override def increaseElementAtPos(pos: BlockPos, elem: BlockInfusionEvents.SoilElement.Value, increase: Int, max: Int = Int.MaxValue): Int = {
    val prev = getElementAtPos(pos, elem)
    val next = Math.min(prev + increase, max)
    setElementAtPos(pos, elem, next)
    next - prev
  }

  // returns the actual change in value after limiting by min (can be negative if min is higher than existing)
  override def decreaseElementAtPos(pos: BlockPos, elem: BlockInfusionEvents.SoilElement.Value, decrease: Int, min: Int = 0): Int = {
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

  // Returns none
  override def removeDataForPos(pos: BlockPos): Option[ElementalBehaviour] = {
    blockData.remove(pos)
    none[ElementalBehaviour]
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


  implicit final class BlockWithInteraction(blockState: BlockState) {
    def hasInfusionBehaviour: Boolean = {
      BlockInfusionEvents.BEHAVIOURS.contains(blockState.getBlock)
    }
    def getInfusionBehaviour = {
      BlockInfusionEvents.BEHAVIOURS.get(blockState.getBlock)
    }
    def isValidToBeAbove: Boolean = {
      if(!blockState.isSolid)
        true
      else
        false
    }
  }

  @tailrec
  private def getBlocksAroundBaseOfPlant(pos: BlockPos, world: IWorld, radius: Int, depth: Int): Iterable[BlockPos] = {
    if(!world.getBlockState(pos.down()).hasInfusionBehaviour) {
      BlockPos.getAllInBoxMutable(pos.add(-radius, -depth, -radius), pos.add(radius, 0, radius)).asScala
    } else {
      getBlocksAroundBaseOfPlant(pos.down(), world, radius, depth)
    }
  }

  private def getRandomPositionAndState(originChunk: Chunk, section: ChunkSection, world: ServerWorld) = {
    val xOrigin = originChunk.getPos.getXStart
    val yOrigin = section.getYLocation
    val zOrigin = originChunk.getPos.getZStart
    val origin = world.getBlockRandomPos(xOrigin, yOrigin, zOrigin, 15)
    val stateAtOrigin = section.getBlockState(origin.getX - xOrigin, origin.getY - yOrigin, origin.getZ - zOrigin)
    (origin, stateAtOrigin)
  }

  override def randomTick(originChunk: Chunk, world : ServerWorld, worldData: WorldData[BlockInfusionInterface]): Unit = {
    for {section <- originChunk.getSections if section != Chunk.EMPTY_SECTION
         _ <- 0 until world.getGameRules.getInt(GameRules.RANDOM_TICK_SPEED)
         (origin, stateAtOrigin) = getRandomPositionAndState(originChunk, section, world)
         allBehaviours <- stateAtOrigin.getInfusionBehaviour.orElse(removeDataForPos(origin))
         } {
      // Infusions by self
      for {
        othPos <- getBlocksAroundBaseOfPlant(origin, world, 1, 0)
        (othChunk, othCap) <- worldData.getOrCache(othPos, originChunk, this)
        othBehaviour <- othChunk.getBlockState(othPos).getInfusionBehaviour
      } {
        for(infuses <- allBehaviours.infuses) {
          othCap.increaseElementAtPos(othPos, infuses, 1, 9)
        }
        for(consumes <- allBehaviours.consumes) {
          if(this.getElementAtPos(origin, consumes) < 9) {
            val consumed = othCap.decreaseElementAtPos(othPos, consumes, 1, 0)
            this.increaseElementAtPos(origin, consumes, consumed, 9)
          }
        }
      }
      // Infusions by fluids
      for {
        fluidPos <- getBlocksAroundBaseOfPlant(origin, world, 4, 1)
        (fluidChunk, fluidCap) <- worldData.getOrCache(fluidPos, originChunk, this)
        checkState = fluidChunk.getFluidState(fluidPos) if !checkState.isEmpty
      } {
        if (checkState.isTagged(FluidTags.WATER)) {
          this.increaseElementAtPos(origin, SoilElement.Water, 1, 9)
        } else if (checkState.isTagged(FluidTags.LAVA)) {
          this.increaseElementAtPos(origin, SoilElement.Fire, 1, 9)
        }
      }
    }

  }


}