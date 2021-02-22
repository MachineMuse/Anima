package net.machinemuse.anima
package blockinfusion

import com.google.gson.{Gson, JsonElement}
import com.mojang.serialization.Codec
import net.minecraft.block.Block
import net.minecraft.client.resources.JsonReloadListener
import net.minecraft.profiler.IProfiler
import net.minecraft.resources.IResourceManager
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IWorld
import net.minecraft.world.chunk.Chunk
import net.minecraftforge.common.IPlantable
import net.minecraftforge.event.AddReloadListenerEvent
import net.minecraftforge.event.world.BlockEvent.CropGrowEvent
import net.minecraftforge.eventbus.api.Event.Result
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import org.apache.logging.log4j.scala.Logging

import blockinfusion.BlockInfusionCapability.BlockInfusionInterface
import util.GenCodecsByName._
import util.VanillaCodecs._
import java.util
import scala.annotation.tailrec
import scala.collection.mutable
import scala.language.implicitConversions
import scala.util.Random

/**
 * Created by MachineMuse on 2/18/2021.
 */
@EventBusSubscriber(modid = Anima.MODID, bus = Bus.FORGE)
object BlockInfusionEvents extends Logging {
  implicit object SoilElement extends Enumeration with CodecByName {
    protected case class SoilElementValue(name: String) extends super.Val(name) {

    }
    implicit def valueToSoilElementVal(x: Value) = x.asInstanceOf[SoilElementValue]

    val Darkness = SoilElementValue("darkness")
    val Air = SoilElementValue("air")
    val Fire = SoilElementValue("fire")
    val Earth = SoilElementValue("earth")
    val Water = SoilElementValue("water")
    val Cold = SoilElementValue("cold")
    val Depletion = SoilElementValue("depletion")
  }

  case class ElementalBehaviour(block: Block,
                                likes: List[SoilElement.Value] = List.empty,
                                dislikes: List[SoilElement.Value] = List.empty,
                                consumes: List[SoilElement.Value] = List.empty,
                                infuses: List[SoilElement.Value] = List.empty) extends CodecByName

  @SubscribeEvent def onCropGrowthPre(event: CropGrowEvent.Pre): Unit = {
    val world = event.getWorld
    val pos = event.getPos
    val state = event.getState
    val block = state.getBlock
    val behaviourOpt = BEHAVIOURS.get(block)
    behaviourOpt.foreach { behaviour =>
      val blocksUnder = getBlocksBelowPlant(pos, world).flatMap[(BlockPos, BlockInfusionInterface)]{pos =>
        if(!world.isBlockLoaded(pos)) {
          none.toSeq
        } else {
          val chunkOpt = world.getChunk(pos.getX >> 4, pos.getZ >> 4).optionallyAs[Chunk]
          val capOpt = chunkOpt.map(_.getCapability(BlockInfusionCapability.getCapability).resolve().get())
          capOpt.fold[Seq[(BlockPos, BlockInfusionInterface)]](Seq.empty) (cap => Seq((pos, cap)))
        }
      }
      if(behaviour.consumes.nonEmpty && blocksUnder.nonEmpty) {
        val consumptionResults = for(consumes <- behaviour.consumes) yield {
          val (bestPos, bestCap) = blocksUnder.maxBy{case (pos, cap) => cap.getElementAtPos(pos, consumes)}
          val bestValue = bestCap.getElementAtPos(bestPos, consumes)
          if(bestValue > 0) {
            bestCap.setElementAtPos(bestPos, consumes, bestValue - 1)
            true
          } else {
            val (leastDepletedPos, leastDepletedCap) = blocksUnder.minBy{case (pos, cap) => cap.getElementAtPos(pos, SoilElement.Depletion)}
            val leastDepletion = leastDepletedCap.getElementAtPos(leastDepletedPos, SoilElement.Depletion)
            if(leastDepletion == 3) {
              // TODO: something more interesting
//              world.setBlockState(leastDepletedPos, Blocks.COARSE_DIRT.getDefaultState, BlockStateFlags.STANDARD_CLIENT_UPDATE)
            } else {
              leastDepletedCap.setElementAtPos(leastDepletedPos, SoilElement.Depletion, leastDepletion + 1)
            }
            false
          }
        }
        val failedConsumptions = consumptionResults.count(!_)
        if(failedConsumptions == 0) {
          event.setResult(Result.ALLOW)
        } else if (Random.nextDouble() < failedConsumptions.toDouble / consumptionResults.size.toDouble) {
          event.setResult(Result.DENY)
        }
      }
    }
  }

  @tailrec
  private def getBlocksBelowPlant(pos: BlockPos, world: IWorld): Seq[BlockPos] = {
    if(!world.getBlockState(pos.down()).getBlock.isInstanceOf[IPlantable]) {
      for {
        x <- Seq(0, -1, 1)
        z <- Seq(0, -1, 1)
      } yield pos.add(x, -1, z)
    } else {
      getBlocksBelowPlant(pos.down(), world)
    }
  }

  private[blockinfusion] val BEHAVIOURS = mutable.HashMap.empty[Block, ElementalBehaviour]

  private val CODEC = implicitly[Codec[ElementalBehaviour]]

  @SubscribeEvent def onAddReloadListeners(event: AddReloadListenerEvent): Unit =
    event.addListener {
      new JsonReloadListener(new Gson, "crop_elements") {

        override def apply(elements: util.Map[ResourceLocation, JsonElement],
                           resources: IResourceManager,
                           profiler: IProfiler): Unit = {
          logger.info(s"Loading crop elemental traits from ${resources.getResourceNamespaces}")
          profiler.startSection("Loading elemental plant data")
          BEHAVIOURS.clear()
          elements.forEach { (resource, json) =>
            val itemOpt = CODEC.parseJson(json)
            itemOpt.fold {
              logger.error(s"Couldn't parse elemental crop traits at ${resource.toString}")
            } { item =>
              BEHAVIOURS.put(item.block, item)
              logger.info(s"Loaded crop ${item.block.getRegistryName} elemental traits from ${resource.toString}")
            }
          }
          profiler.endSection()
        }

      }
    }
}
