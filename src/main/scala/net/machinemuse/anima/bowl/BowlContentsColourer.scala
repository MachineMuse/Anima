package net.machinemuse.anima
package bowl

import net.minecraft.client.renderer.color.IItemColor
import net.minecraft.item.ItemStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import org.apache.logging.log4j.scala.Logging

/**
 * Created by MachineMuse on 2/9/2021.
 */
@EventBusSubscriber(modid = Anima.MODID, value = Array(Dist.CLIENT), bus = Bus.MOD)
object BowlContentsColourer extends IItemColor with Logging {


  override def getColor(stack : ItemStack, tintIndex : Int): Int = {
    if(tintIndex > 0) {
      stack.getItem.mapAsOrElse[BowlWithContents, Int] (-1) { _.contentsColour }
    } else {
      -1
    }
  }
}
