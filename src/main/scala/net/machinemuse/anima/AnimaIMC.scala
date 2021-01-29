package net.machinemuse.anima

import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.InterModComms
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.{InterModEnqueueEvent, InterModProcessEvent}
import org.apache.logging.log4j.LogManager

/**
 * Created by MachineMuse on 1/28/2021.
 */
@Mod.EventBusSubscriber(modid = Anima.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
object AnimaIMC {
  private val LOGGER = LogManager.getLogger


  @SubscribeEvent
  def enqueueIMC(event: InterModEnqueueEvent): Unit = { // some example code to dispatch IMC to another mod
    InterModComms.sendTo("examplemod", "helloworld", () => {
      def foo() = {
        LOGGER.trace("Hello world from the MDK")
        "Hello world"
      }

      foo()
    })
  }

  @SubscribeEvent
  def processIMC(event: InterModProcessEvent): Unit = { // some example code to receive and process InterModComms from other mods
    val imcStream = event.getIMCStream.map((m: InterModComms.IMCMessage) => m.getMessageSupplier.get)
    LOGGER.trace("Got IMC {}", imcStream /*.collect(Collectors.toList) */ )
  }

}
