package net.machinemuse.anima
package blockinfusion

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.util._
import net.minecraft.util.math.{RayTraceContext, RayTraceResult}
import net.minecraft.util.text.StringTextComponent
import net.minecraft.world.World
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.{FMLConstructModEvent, GatherDataEvent}

import registration.RegistryHelpers
import registration.RegistryHelpers.regExtendedItem
import util.DatagenHelpers.{mkLanguageProvider, mkSimpleItemModel}
import util.Logging

/**
 * Created by MachineMuse on 2/21/2021.
 */
object DebugStaff extends Logging {
  @SubscribeEvent def onConstructMod(event: FMLConstructModEvent) = {}

  private val props = RegistryHelpers.DEFAULT_ITEM_PROPERTIES.maxStackSize(1)

  val ITEM = regExtendedItem("debug_staff", () => new DebugStaff(props))

  @SubscribeEvent def gatherData(implicit event: GatherDataEvent): Unit = {

    mkLanguageProvider("en_us"){ lang =>
      lang.addItem(ITEM, "Debug Staff")
    }
    mkLanguageProvider("fr_fr"){ lang =>
      lang.addItem(ITEM, "Bâton de Débogage")
    }
    mkSimpleItemModel(ITEM.get, "item/summoningstaff")
  }

}

@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
class DebugStaff(properties: Item.Properties) extends Item(properties) {

  override def onItemRightClick(world : World, player : PlayerEntity, hand : Hand): ActionResult[ItemStack] = {
    val rayTraceResult = rayTrace(world, player, RayTraceContext.FluidMode.ANY)
    val itemInHand = player.getHeldItem(hand)
    rayTraceResult.getType match {
      case RayTraceResult.Type.BLOCK =>
        world.onServer { sw =>
          val pos = rayTraceResult.getPos
          val chunk = world.getChunkAt(pos)
          val cap = chunk.getCapability(BlockInfusionCapability.getCapability)
          val capData = cap.resolve.get.getDataForPos(pos)
          val blockState = chunk.getBlockState(pos)
          player.sendMessage(new StringTextComponent(s"Block ${blockState.getBlock} elemental data: $capData"), Util.DUMMY_UUID)
        }
        ActionResult.resultSuccess(itemInHand)
      case RayTraceResult.Type.ENTITY => ActionResult.resultPass(itemInHand)
      case RayTraceResult.Type.MISS => ActionResult.resultPass(itemInHand)

    }
  }

}
