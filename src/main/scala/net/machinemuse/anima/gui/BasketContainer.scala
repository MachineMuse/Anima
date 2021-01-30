package net.machinemuse.anima
package gui

import net.machinemuse.anima.item.basket.Basket
import net.machinemuse.anima.registration.AnimaRegistry
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.PacketBuffer
import net.minecraft.util.Hand
import org.apache.logging.log4j.scala.Logging

/**
 * Created by MachineMuse on 1/22/2021.
 */
class BasketContainer(windowID: Int, playerInventory: PlayerInventory, hand: Hand) extends HeldItemContainer(playerInventory, hand, AnimaRegistry.BASKET_ITEM.get(), AnimaRegistry.BASKET_CONTAINER.get(), windowID) with Logging {
  def this(windowId: Int, playerInventory: PlayerInventory, buf: PacketBuffer) = {
    this(windowId, playerInventory, Hand.values()(buf.readByte()))
  }
  val containerItem: Basket = AnimaRegistry.BASKET_ITEM.get

  val numInnerSlots = 9

  for (x <- 0 until 3) {
    for (y <- 0 until 3) {
      this.addSlot(mkInnerSlot(y + x * 3, 62 + y * 18, 17 + x * 18))
    }
  }


  mkPlayerInventorySlots(8, 84, basketSlotNumber)



}
