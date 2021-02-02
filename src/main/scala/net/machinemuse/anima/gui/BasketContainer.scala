package net.machinemuse.anima
package gui

import gui.BasketContainer.getType
import item.basket.Basket
import registration.RegistryHelpers._

import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.PacketBuffer
import net.minecraft.util.Hand
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent
import org.apache.logging.log4j.scala.Logging

/**
 * Created by MachineMuse on 1/22/2021.
 */
object BasketContainer {

  @SubscribeEvent def onConstructMod(event: FMLConstructModEvent) = {}
  val typeInstance = regContainerType("basket", (id, inv, buf) => new BasketContainer(id, inv, buf))
  def getType = typeInstance.get()
}

@Mod.EventBusSubscriber(modid = Anima.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
class BasketContainer(windowID: Int, playerInventory: PlayerInventory, hand: Hand) extends HeldItemContainer(playerInventory, hand, Basket.getInstance, getType, windowID) with Logging {
  for (x <- 0 until 3) {
    for (y <- 0 until 3) {
      this.addSlot(mkInnerSlot(y + x * 3, 62 + y * 18, 17 + x * 18))
    }
  }
  mkPlayerInventorySlots(8, 84, basketSlotNumber)

  // In case this container was produced via a packet, we have to find out which hand was used by reading it from the packet's extra info
  def this(windowId: Int, playerInventory: PlayerInventory, buf: PacketBuffer) = {
    this(windowId, playerInventory, Hand.values()(buf.readByte()))
  }
}
