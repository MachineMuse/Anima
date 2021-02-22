package net.machinemuse.anima
package campfire

import net.minecraft.client.renderer.color.IItemColor
import net.minecraft.item.{DyeColor, ItemStack}
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.ColorHandlerEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus

import util.Logging

/**
 * Created by MachineMuse on 2/7/2021.
 */
@EventBusSubscriber(modid = Anima.MODID, value = Array(Dist.CLIENT), bus = Bus.MOD)
object CampfireDustColour extends IItemColor with Logging {
  @SubscribeEvent def onItemColorEvent(event: ColorHandlerEvent.Item) = {
    event.getItemColors.register(this, DustForCampfire.CAMPFIRE_DUST_ITEM.get)
  }

  val defaultColour = DyeColor.GREEN.getTextColor
  override def getColor(stack : ItemStack, tintIndex : Int): Int = {
    if(stack.hasTag && stack.getTag.contains(s"colour$tintIndex")) {
      stack.getTag.getInt(s"colour$tintIndex")
    } else {
      -1
    }
  }
}
