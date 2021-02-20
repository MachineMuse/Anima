package net.machinemuse.anima
package farmland

import com.google.gson.{Gson, JsonElement}
import com.mojang.serialization.Codec
import net.minecraft.block.Block
import net.minecraft.client.resources.JsonReloadListener
import net.minecraft.profiler.IProfiler
import net.minecraft.resources.IResourceManager
import net.minecraft.state.IntegerProperty
import net.minecraft.util.ResourceLocation
import net.minecraftforge.event.AddReloadListenerEvent
import net.minecraftforge.event.world.BlockEvent.CropGrowEvent
import net.minecraftforge.eventbus.api.Event.Result
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import org.apache.logging.log4j.scala.Logging

import constants.BlockStateFlags
import util.GenCodecsByName._
import util.VanillaClassEnrichers.RichBlockState
import util.VanillaCodecs._
import java.util
import scala.collection.mutable
import scala.language.implicitConversions

/**
 * Created by MachineMuse on 2/18/2021.
 */
@EventBusSubscriber(modid = Anima.MODID, bus = Bus.FORGE)
object FarmlandInfusionEvents extends Logging {
  implicit object SoilElement extends Enumeration with CodecByName {
    protected case class SoilElementValue(name: String, property: IntegerProperty) extends super.Val(name) {

    }
    implicit def valueToSoilElementVal(x: Value) = x.asInstanceOf[SoilElementValue]

    val Darkness = SoilElementValue("darkness", InfusedBlock.INFUSED_DARKNESS)
    val Air = SoilElementValue("air", InfusedBlock.INFUSED_AIR)
    val Fire = SoilElementValue("fire", InfusedBlock.INFUSED_FIRE)
    val Earth = SoilElementValue("earth", InfusedBlock.INFUSED_EARTH)
    val Water = SoilElementValue("water", InfusedBlock.INFUSED_WATER)
    val Cold = SoilElementValue("cold", InfusedBlock.INFUSED_COLD)
  }

  case class ElementalBehaviour(block: Block,
                                likes: List[SoilElement.Value] = List.empty,
                                dislikes: List[SoilElement.Value] = List.empty,
                                consumes: List[SoilElement.Value] = List.empty,
                                infuses: List[SoilElement.Value] = List.empty) extends CodecByName

  private val BEHAVIOURS = mutable.HashMap.empty[Block, ElementalBehaviour]

  private val CODEC = implicitly[Codec[ElementalBehaviour]]
  @SubscribeEvent def onCropGrowthPre(event: CropGrowEvent.Pre): Unit = {
    val world = event.getWorld
    val pos = event.getPos
    val state = event.getState
    val block = state.getBlock
    val behaviourOpt = BEHAVIOURS.get(block)
    for {
      behaviour <- behaviourOpt.toList
      x <- -1 to 1
      z <- -1 to 1
    } {
      val scanningPos = pos.add(x, -1, z)
      val scanningState = world.getBlockState(scanningPos)
      for(consume <- behaviour.consumes) {
        if(event.getResult != Result.ALLOW) {
          scanningState.getBlock.optionallyDoAs[InfusedFarmland]{ farmlandBlock =>
            val amount = scanningState.get(consume.property).intValue()
            if(amount > 0) {
              world.setBlockState(scanningPos, scanningState.updated(consume.property, amount-1), BlockStateFlags.STANDARD_CLIENT_UPDATE)
              event.setResult(Result.ALLOW)
            }
          }
        }
      }
    }
  }

  @SubscribeEvent def onCropGrowthPost(event: CropGrowEvent.Post): Unit = {
    val world = event.getWorld
    val pos = event.getPos
    val state = event.getState
    val block = state.getBlock
    val behaviourOpt = BEHAVIOURS.get(block)

    for {
      behaviour <- behaviourOpt.toList
      x <- -1 to 1
      z <- -1 to 1
    } {
      val scanningPos = pos.add(x, -1, z)
      val scanningState = world.getBlockState(scanningPos)
      for (infuse <- behaviour.infuses) {
        scanningState.getBlock.optionallyDoAs[InfusedFarmland] { farmlandBlock =>
          val amount = scanningState.get(infuse.property).intValue()
          if (amount < 3) {
            world.setBlockState(scanningPos, scanningState.updated(infuse.property, amount + 1), BlockStateFlags.STANDARD_CLIENT_UPDATE)
            event.setResult(Result.ALLOW)
          }
        }
      }
    }

  }

  @SubscribeEvent def onAddReloadListeners(event: AddReloadListenerEvent): Unit =
    event.addListener {
      new JsonReloadListener(new Gson, "crop_elements") {

        override def apply(elements: util.Map[ResourceLocation, JsonElement],
                           resources: IResourceManager,
                           profiler: IProfiler): Unit = {
          logger.info(s"Loading crop elemental traits from ${resources.getResourceNamespaces}")
          profiler.startSection("Loading elemental plant data")
          elements.forEach { (resource, json) =>
            val itemOpt = CODEC.parseJson(json)
            itemOpt.fold {
              logger.error(s"Couldn't parse elemental crop traits at ${resource.getPath}")
            } { item =>
              BEHAVIOURS.put(item.block, item)
              logger.info(s"Loaded crop ${item.block.getRegistryName} elemental traits from ${resource.getPath}")
            }
          }
          profiler.endSection()
        }

      }
    }
}
