package net.machinemuse.anima

import net.minecraftforge.fml.common.Mod
import org.apache.logging.log4j.LogManager

/**
 * Created by MachineMuse on 1/20/2021.
 */
object Anima {
  final val MODID = "anima"
}
@Mod(Anima.MODID)
class Anima {
  private val LOGGER = LogManager.getLogger

//  FMLJavaModLoadingContext.get.getModEventBus.addListener(respondToModEvents)
//  def respondToModEvents(event: ModLifecycleEvent) = {
//    LOGGER.info("Got mod event: " + event)
//  }

//
//  MinecraftForge.EVENT_BUS.addListener(respondToForgeEvents)
//  def respondToForgeEvents(event: Event) = {
//    LOGGER.info("Got forge event: " + event)
//  }
//
//  @SubscribeEvent
//  def setup(event: FMLCommonSetupEvent): Unit = { // some preinit code
//    LOGGER.info("HELLO FROM PREINIT")
//    LOGGER.info("DIRT BLOCK >> {}", Blocks.DIRT.getRegistryName)
////    event.enqueueWork { () =>
////      LOGGER.info("Attributes Created")
////    }
//  }
//
//  // You can use SubscribeEvent and let the Event Bus discover methods to call
//  @SubscribeEvent def onServerStarting(event: FMLServerStartingEvent): Unit = { // do something when the server starts; this is on the FORGE event bus so be careful
//    LOGGER.info("HELLO from server starting")
//  }

}
