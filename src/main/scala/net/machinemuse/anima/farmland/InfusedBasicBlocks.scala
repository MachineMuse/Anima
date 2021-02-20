package net.machinemuse.anima
package farmland

import net.minecraft.block.material.{Material, MaterialColor}
import net.minecraft.block._
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent
import org.apache.logging.log4j.scala.Logging

import farmland.InfusedBlock.InfusedBlock
import registration.RegistryHelpers.{regBlock, regSimpleBlockItem}

/**
 * Created by MachineMuse on 2/19/2021.
 */
@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
object InfusedBasicBlocks extends Logging {
  @SubscribeEvent def onConstructMod(e: FMLConstructModEvent) = {}

  private val DIRT_PROPERTIES = AbstractBlock.Properties.create(Material.EARTH, MaterialColor.DIRT).hardnessAndResistance(0.5F).sound(SoundType.GROUND)
  val DIRT = regBlock("infused_dirt", () => new InfusedBlock(Blocks.DIRT, DIRT_PROPERTIES))
  val DIRT_ITEM = regSimpleBlockItem("infused_dirt", DIRT)
}
