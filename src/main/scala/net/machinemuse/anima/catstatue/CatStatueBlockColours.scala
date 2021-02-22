package net.machinemuse.anima
package catstatue

import net.minecraft.block.BlockState
import net.minecraft.block.material.MaterialColor
import net.minecraft.client.renderer.color.IBlockColor
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockDisplayReader
import net.minecraft.world.biome.BiomeColors
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.ColorHandlerEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus

import util.Logging

/**
 * Created by MachineMuse on 2/11/2021.
 */

@EventBusSubscriber(modid = Anima.MODID, value = Array(Dist.CLIENT), bus = Bus.MOD)
object CatStatueBlockColours extends IBlockColor with Logging {
  @SubscribeEvent def onBlockColorEvent(event: ColorHandlerEvent.Block) = {
    event.getBlockColors.register(this, CatStatue.BLOCK.get)
  }

  override def getColor(state : BlockState, world : IBlockDisplayReader, pos : BlockPos, tintIndex : Int): Int = {
    if(tintIndex == 1 && world != null && pos != null) {
      BiomeColors.getWaterColor(world, pos)
    } else if(tintIndex == 1) {
      MaterialColor.WATER.colorValue
    } else {
      -1
    }
  }
}
