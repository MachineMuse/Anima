package net.machinemuse.anima.item

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.container.Slot
import net.minecraft.item.ItemStack

/**
 * Created by MachineMuse on 1/22/2021.
 */
class LockedSlot(inventoryIn: IInventory, index: Int, xPosition: Int, yPosition: Int) extends Slot(inventoryIn, index, xPosition, yPosition) {
  override def canTakeStack(playerIn: PlayerEntity): Boolean = false

  override def isItemValid(stack: ItemStack): Boolean = false

  // This is used by rendering for various things
  //  override def isEnabled: Boolean = false
}
