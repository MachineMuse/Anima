package net.machinemuse.anima
package entity

import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent

import util.DatagenHelpers._
import util.Logging

/**
 * Created by MachineMuse on 2/22/2021.
 */
@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
object LightSpiritDatagen extends Logging {
  @SubscribeEvent
  def gatherData(implicit event: GatherDataEvent): Unit = {
    mkSimpleBlockState(
      EntityLightSpirit.AIRLIGHT_BLOCK.get,
      existingVanillaModelFile("block/air")
    )
  }
}
