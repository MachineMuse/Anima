package net.machinemuse.anima
package registration

import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent

/**
 * Created by MachineMuse on 1/21/2021.
 */
@Mod.EventBusSubscriber(modid = Anima.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
object AnimaRegistry {
  import RegistryHelpers._

  @SubscribeEvent def init(e: FMLConstructModEvent) = {}

  val SPIRITFIRE_ITEM = regSimpleItem("spiritfire", ItemProperties(creativeGroup = Some(null)).some)
  val AnimaCreativeGroup = regCreativeTab(() => SPIRITFIRE_ITEM)

  val KINDLING_ITEM = regSimpleItem("kindling")

  val BIRDBONES_ITEM = regSimpleItem("birdbones")

  val ANIMALBONES_ITEM = regSimpleItem("animalbones")

  val GHOSTDUST_ITEM = regSimpleItem("ghost_dust")

  val GHOSTDUST_REMOVER_ITEM = regSimpleItem("ghost_dust_remover")


//  // You can do it this way instead
//  @Mod.EventBusSubscriber(modid = Anima.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
//  object RegistryEvents {
//    @SubscribeEvent def onBlocksRegistry(blockRegistryEvent: RegistryEvent.Register[Block]): Unit = { // register a new block here
//      LOGGER.info("HELLO from Register Block")
//    }
//
//    @SubscribeEvent def onItemsRegistry(itemRegistryEvent: RegistryEvent.Register[Item]): Unit = {
//      LOGGER.info("HELLO from Register Item")
//    }
//  }
}
