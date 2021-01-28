package net.machinemuse.anima

import net.machinemuse.anima.client.ClientForgeEvents
import net.machinemuse.anima.item.basket.BasketEvents
import net.machinemuse.anima.registration.AnimaRegistry
import net.minecraft.block.{Block, Blocks}
import net.minecraft.item.Item
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.InterModComms
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.{FMLCommonSetupEvent, InterModEnqueueEvent, InterModProcessEvent}
import net.minecraftforge.fml.event.server.FMLServerStartingEvent
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import org.apache.logging.log4j.LogManager

/**
 * Created by MachineMuse on 1/20/2021.
 */
object Anima {
  final val MODID = "anima"
}
@Mod(Anima.MODID)
class Anima {
  // Directly reference a log4j logger.
  private val LOGGER = LogManager.getLogger

  // Register the setup method for modloading
  FMLJavaModLoadingContext.get.getModEventBus.addListener(this.setup)
  // Register the enqueueIMC method for modloading
  FMLJavaModLoadingContext.get.getModEventBus.addListener(this.enqueueIMC)
  // Register the processIMC method for modloading
  FMLJavaModLoadingContext.get.getModEventBus.addListener(this.processIMC)
  // Register ourselves for server and other game events we are interested in
  MinecraftForge.EVENT_BUS.register(this)
  MinecraftForge.EVENT_BUS.register(new BasketEvents)
  MinecraftForge.EVENT_BUS.register(ClientForgeEvents)


  AnimaRegistry

  private def setup(event: FMLCommonSetupEvent): Unit = { // some preinit code
    LOGGER.info("HELLO FROM PREINIT")
    LOGGER.info("DIRT BLOCK >> {}", Blocks.DIRT.getRegistryName)
//    event.enqueueWork { () =>
//      LOGGER.info("Attributes Created")
//    }
  }

  private def enqueueIMC(event: InterModEnqueueEvent): Unit = { // some example code to dispatch IMC to another mod
    InterModComms.sendTo("examplemod", "helloworld", () => {
      def foo() = {
        LOGGER.info("Hello world from the MDK")
        "Hello world"
      }

      foo()
    })
  }

  private def processIMC(event: InterModProcessEvent): Unit = { // some example code to receive and process InterModComms from other mods
    val imcStream = event.getIMCStream.map((m: InterModComms.IMCMessage) => m.getMessageSupplier.get)
    LOGGER.info("Got IMC {}", imcStream /*.collect(Collectors.toList) */ )
  }

  // You can use SubscribeEvent and let the Event Bus discover methods to call
  @SubscribeEvent def onServerStarting(event: FMLServerStartingEvent): Unit = { // do something when the server starts
    LOGGER.info("HELLO from server starting")
  }

  // You can use EventBusSubscriber to automatically subscribe events on the contained class (this is subscribing to the MOD
  // Event bus for receiving Registry Events)
  @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD) object RegistryEvents {
    @SubscribeEvent def onBlocksRegistry(blockRegistryEvent: RegistryEvent.Register[Block]): Unit = { // register a new block here
      LOGGER.info("HELLO from Register Block")
    }

    @SubscribeEvent def onItemsRegistry(itemRegistryEvent: RegistryEvent.Register[Item]): Unit = {
      LOGGER.info("HELLO from Register Item")
    }
  }
}
