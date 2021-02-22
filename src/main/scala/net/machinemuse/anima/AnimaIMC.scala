package net.machinemuse.anima

import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.InterModComms
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.{InterModEnqueueEvent, InterModProcessEvent}

import util.Logging

/**
 * Created by MachineMuse on 1/28/2021.
 */
@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
object AnimaIMC extends Logging {


  @SubscribeEvent
  def enqueueIMC(event: InterModEnqueueEvent): Unit = { // some example code to dispatch IMC to another mod
    InterModComms.sendTo("examplemod", "helloworld", () => {
      def foo() = {
        logger.debug("Hello world from the MDK")
        "Hello world"
      }

      foo()
    })
  }

  @SubscribeEvent
  def processIMC(event: InterModProcessEvent): Unit = { // some example code to receive and process InterModComms from other mods
    val imcStream = event.getIMCStream.map((m: InterModComms.IMCMessage) => m.getMessageSupplier.get)
    logger.debug(s"Got IMC {$imcStream}"/*.collect(Collectors.toList) */ )
  }

}
