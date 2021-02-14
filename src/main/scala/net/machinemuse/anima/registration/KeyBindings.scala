package net.machinemuse.anima
package registration
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent

/**
 * Created by MachineMuse on 1/27/2021.
 */
@EventBusSubscriber(modid = Anima.MODID, value = Array(Dist.CLIENT), bus = Bus.MOD)
object KeyBindings {

  @SubscribeEvent
  def onClientSetup(event: FMLClientSetupEvent) : Unit = {
    for( key <- KeyBindings.keybinds) {
      ClientRegistry.registerKeyBinding(key)
    }
  }

  val keybinds = List(
//    new KeyBinding("increase.translatex", keyType, keyboard_keypad_1, "Debug"),
//    new KeyBinding("increase.translatey", keyType, keyboard_keypad_2, "Debug"),
//    new KeyBinding("increase.translatez", keyType, keyboard_keypad_3, "Debug"),
//    new KeyBinding("decrease.translatex", keyType, keyboard_keypad_4, "Debug"),
//    new KeyBinding("decrease.translatey", keyType, keyboard_keypad_5, "Debug"),
//    new KeyBinding("decrease.translatez", keyType, keyboard_keypad_6, "Debug"),
//    new KeyBinding("increase.scalex", keyType, keyboard_keypad_7, "Debug")    ,
//    new KeyBinding("increase.scaley", keyType, keyboard_keypad_8, "Debug")    ,
//    new KeyBinding("decrease.scalex", keyType, keyboard_keypad_9, "Debug")    ,
//    new KeyBinding("decrease.scaley", keyType, keyboard_keypad_0, "Debug")
  )


}
