package net.machinemuse.anima
package plants

import net.minecraft.block.material.Material
import net.minecraft.block._
import net.minecraft.client.renderer.{RenderType, RenderTypeLookup}
import net.minecraftforge.api.distmarker.{Dist, OnlyIn}
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.{FMLClientSetupEvent, FMLConstructModEvent}
import org.apache.logging.log4j.scala.Logging

import registration.RegistryHelpers._

/**
 * Created by MachineMuse on 2/19/2021.
 */
@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
object Radish extends Logging {

  @SubscribeEvent def onConstructMod(event: FMLConstructModEvent) = {}

  @OnlyIn(Dist.CLIENT) @SubscribeEvent def onClientSetup(event: FMLClientSetupEvent) = {
    RenderTypeLookup.setRenderLayer(RADISH_BLOCK.get, RenderType.getCutout)
  }

  protected val properties = AbstractBlock.Properties.create(Material.PLANTS)
    .doesNotBlockMovement
    .tickRandomly
    .zeroHardnessAndResistance
    .sound(SoundType.PLANT)

  val RADISH_BLOCK = regBlock("radish", () => new CropsBlock(properties))

  val RADISH_ITEM = regSimpleItem("radish")
  val RADISH_SEEDS_ITEM = regNamedBlockItem("radish_seeds", RADISH_BLOCK)

}
