package net.machinemuse.anima
package bowl

import net.minecraft.client.renderer.color.IItemColor
import net.minecraft.item.ItemStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.ColorHandlerEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus

import bowl.BowlWithContents.BOWL_WITH_CONTENTS
import util.Logging

/**
 * Created by MachineMuse on 2/9/2021.
 */
@EventBusSubscriber(modid = Anima.MODID, value = Array(Dist.CLIENT), bus = Bus.MOD)
object BowlContentsColourer extends IItemColor with Logging {

  @SubscribeEvent def onItemColorEvent(event: ColorHandlerEvent.Item) = {
    event.getItemColors.register(BowlContentsColourer, BOWL_WITH_CONTENTS.get)
  }

  override def getColor(stack : ItemStack, tintIndex : Int): Int = {
    if(tintIndex > 0) {
      BowlContents.getContents(stack).getColour
    } else {
      -1
    }
  }
}
