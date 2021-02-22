package net.machinemuse.anima
package basket

import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.PacketBuffer
import net.minecraft.util.Hand
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent

import basket.BasketContainer.BASKET_CONTAINER
import gui.HeldItemContainer
import registration.RegistryHelpers._
import util.Logging

/**
 * Created by MachineMuse on 1/22/2021.
 */
object BasketContainer {

  @SubscribeEvent def onConstructMod(event: FMLConstructModEvent) = {}
  val BASKET_CONTAINER = regContainerType("basket", (id, inv, buf) => new BasketContainer(id, inv, buf))
}

@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
class BasketContainer(windowID: Int, playerInventory: PlayerInventory, hand: Hand) extends HeldItemContainer(playerInventory, hand, Basket.BASKET_ITEM.get, BASKET_CONTAINER.get, windowID) with Logging {
  for (x <- 0 until 3) {
    for (y <- 0 until 3) {
      this.addSlot(mkInnerSlot(y + x * 3, 62 + y * 18, 17 + x * 18))
    }
  }
  mkPlayerInventorySlots(8, 84)

  // In case this container was produced via a packet, we have to find out which hand was used by reading it from the packet's extra info
  def this(windowId: Int, playerInventory: PlayerInventory, buf: PacketBuffer) = {
    this(windowId, playerInventory, Hand.values()(buf.readByte()))
  }
}
