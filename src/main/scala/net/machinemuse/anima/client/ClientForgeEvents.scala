package net.machinemuse.anima.client

import net.machinemuse.anima.item.basket.BasketISTER
import net.minecraft.client.Minecraft
import org.apache.logging.log4j.LogManager

/**
 * Created by MachineMuse on 1/27/2021.
 */
object ClientForgeEvents {
  private val LOGGER = LogManager.getLogger

  def logTranslations() ={
    Minecraft.getInstance().player.sendChatMessage("Translate = " + (BasketISTER.translateX, BasketISTER.translateY, BasketISTER.translateZ).toString)
    Minecraft.getInstance().player.sendChatMessage("Scale = " + (BasketISTER.scaleX, BasketISTER.scaleY, BasketISTER.scaleZ).toString)
  }


//  @SubscribeEvent
//  def onKeyEvent(event: KeyInputEvent): Unit = {
//    if(event.getAction == GLFW.GLFW_PRESS)
//      event.getKey match {
//        case KeyBindings.KEYBOARD_KEYPAD_1 =>
//          BasketISTER.translateX = BasketISTER.translateX + 1.0F/16.0F
//          logTranslations()
//
//        case KeyBindings.KEYBOARD_KEYPAD_2 =>
//          BasketISTER.translateY = BasketISTER.translateY + 1.0F/16.0F
//          logTranslations()
//
//        case KeyBindings.KEYBOARD_KEYPAD_3 =>
//          BasketISTER.translateZ = BasketISTER.translateZ + 1.0F/16.0F
//          logTranslations()
//
//        case KeyBindings.KEYBOARD_KEYPAD_4 =>
//          BasketISTER.translateX = BasketISTER.translateX - 1.0F/16.0F
//          logTranslations()
//
//        case KeyBindings.KEYBOARD_KEYPAD_5 =>
//          BasketISTER.translateY = BasketISTER.translateY - 1.0F/16.0F
//          logTranslations()
//
//        case KeyBindings.KEYBOARD_KEYPAD_6 =>
//          BasketISTER.translateZ = BasketISTER.translateZ - 1.0F/16.0F
//          logTranslations()
//
//        case KeyBindings.KEYBOARD_KEYPAD_7 =>
//          BasketISTER.scaleX = BasketISTER.scaleX + 1.0F/16.0F
//          logTranslations()
//
//        case KeyBindings.KEYBOARD_NUM_LOCK =>
//          BasketISTER.scaleX = BasketISTER.scaleX - 1.0F/16.0F
//          logTranslations()
//
//        case KeyBindings.KEYBOARD_KEYPAD_8 =>
//          BasketISTER.scaleY = BasketISTER.scaleY + 1.0F/16.0F
//          logTranslations()
//
//        case KeyBindings.KEYBOARD_KEYPAD_DIVIDE =>
//          BasketISTER.scaleY = BasketISTER.scaleY - 1.0F/16.0F
//          logTranslations()
//
//        case KeyBindings.KEYBOARD_KEYPAD_9 =>
//          BasketISTER.scaleZ = BasketISTER.scaleZ + 1.0F/16.0F
//          logTranslations()
//
//        case KeyBindings.KEYBOARD_KEYPAD_MULTIPLY =>
//          BasketISTER.scaleZ = BasketISTER.scaleZ - 1.0F/16.0F
//          logTranslations()
//
//
//        case _ =>
//      }
//  }
}
