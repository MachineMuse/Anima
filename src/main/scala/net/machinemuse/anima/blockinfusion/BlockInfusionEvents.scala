package net.machinemuse.anima
package blockinfusion

import com.google.gson.{Gson, JsonElement}
import com.mojang.serialization.Codec
import net.minecraft.block.Block
import net.minecraft.client.resources.JsonReloadListener
import net.minecraft.profiler.IProfiler
import net.minecraft.resources.IResourceManager
import net.minecraft.util.ResourceLocation
import net.minecraft.world.chunk.Chunk
import net.minecraftforge.event.AddReloadListenerEvent
import net.minecraftforge.event.world.BlockEvent.CropGrowEvent
import net.minecraftforge.eventbus.api.Event.Result
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus

import blockinfusion.BlockInfusionCapability.ChunkWithCapability
import util.GenCodecsByName._
import util.Logging
import util.VanillaCodecs._
import java.util
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

  private val CODEC = implicitly[Codec[ElementalBehaviour]]

  @SubscribeEvent def onCropGrowthPre(event: CropGrowEvent.Pre): Unit = {
    val world = event.getWorld
    val pos = event.getPos
    val state = event.getState
    val block = state.getBlock
    for {
      behaviour <- BEHAVIOURS.get(block)
      chunk <- world.getChunk(pos).optionallyAs[Chunk]
      cap = chunk.getAndRealizeCapability(BlockInfusionCapability.getCapability)
    } {
      val consumptions = behaviour.consumes.map {consumes =>
        if(cap.getElementAtPos(pos, consumes) > 0) {
          cap.decreaseElementAtPos(pos, consumes, 1, 0)
        } else {
          0
        }
      }
      val failedConsumptions = consumptions.count(_ == 0)
      if(failedConsumptions == 0) {
        event.setResult(Result.ALLOW)
      } else if (Random.nextDouble() < failedConsumptions.toDouble / consumptions.size.toDouble) {
        event.setResult(Result.DENY)
      }
    }
  }

  private[blockinfusion] val BEHAVIOURS = mutable.HashMap.empty[Block, ElementalBehaviour]

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
