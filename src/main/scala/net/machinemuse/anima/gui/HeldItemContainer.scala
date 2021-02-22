package net.machinemuse.anima
package gui

import net.minecraft.entity.player.{PlayerEntity, PlayerInventory}
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.container._
import net.minecraft.item.ItemStack
import net.minecraft.util.Hand
import net.minecraftforge.items.{IItemHandler, SlotItemHandler}

import scala.annotation.nowarn
import scala.jdk.CollectionConverters.ListHasAsScala

import item.InventoriedItem
import util.Logging

/**
 * Created by MachineMuse on 1/29/2021.
 */
object HeldItemContainer extends Logging {
}

abstract class HeldItemContainer(playerInventory: PlayerInventory, hand: Hand, containerItem: InventoriedItem, ct: ContainerType[_], id: Int) extends Container(ct, id) with Logging {
  override def canInteractWith(playerIn: PlayerEntity): Boolean = true

  val (usedInventory, usedInventorySlot, activeSlot) = hand match {
    case Hand.MAIN_HAND => (playerInventory.mainInventory, playerInventory.currentItem, playerInventory.currentItem)
    case Hand.OFF_HAND => (playerInventory.offHandInventory, 0, 40)
  }

  val containerItemStack: ItemStack = usedInventory.get(usedInventorySlot)

  val innerInventory: IItemHandler = containerItem.createInventory(containerItemStack)

  def numInnerSlots: Int = innerInventory.getSlots

  def mkInnerSlot(index: Int, xPosition: Int, yPosition: Int) = {
    new SlotItemHandler(innerInventory, index, xPosition, yPosition) {
//      override def isItemValid(stack: ItemStack): Boolean = containerItem.canStoreItem(containerItemStack, stack)
//      override def getItemStackLimit(stack: ItemStack): Int = containerItem.getStackLimit(stack)
    }
  }

  class LockedSlot(inventoryIn: IInventory, index: Int, xPosition: Int, yPosition: Int) extends Slot(inventoryIn, index, xPosition, yPosition) {
    override def canTakeStack(playerIn: PlayerEntity): Boolean = false
    override def isItemValid(stack: ItemStack): Boolean = false
    // This is used by rendering for various things
    //  override def isEnabled: Boolean = false
  }

  def mkPlayerInventorySlots(x: Int, y: Int) = {
    for (k <- 0 until 3) {
      for (i1 <- 0 until 9) {
        this.addSlot(new Slot(playerInventory, i1 + k * 9 + 9, x + i1 * 18, y + k * 18))
      }
    }

    for (l <- 0 until 9) {
      if(l == activeSlot) {
        this.addSlot(new LockedSlot(playerInventory, l, x + l * 18, y + 58))
      } else {
        this.addSlot(new Slot(playerInventory, l, x + l * 18, y + 58))
      }
    }
  }

  def betterMergeItemStack(stackToMerge: ItemStack, startIndex: Int, endIndex: Int, reverseDirection: Boolean = false): ItemStack = {
    val slotsChosen = inventorySlots.subList(startIndex, endIndex).asScala
    val slots = if(reverseDirection) slotsChosen.reverse else slotsChosen

    val stackLeftAfterMatching = slots.foldLeft(stackToMerge) {(stackRemaining, currSlot) =>
      val currSlotStack = currSlot.getStack
      if(currSlot.getHasStack && Container.areItemsAndTagsEqual(stackRemaining, currSlot.getStack) && currSlot.isItemValid(stackRemaining)) {
        val numToPut = Math.min(currSlot.getSlotStackLimit - currSlotStack.getCount, stackRemaining.getCount)
        currSlotStack.grow(numToPut)
        stackRemaining.shrink(numToPut)
        currSlot.putStack(currSlotStack)
        currSlot.onSlotChanged()
        stackRemaining
      } else {
        stackRemaining
      }
    }
    val stackLeftAfterFilling = slots.foldLeft(stackLeftAfterMatching) {(stackRemaining, currSlot) =>
      if(!currSlot.getHasStack && currSlot.isItemValid(stackRemaining)){
        val numToPut = Math.min(currSlot.getSlotStackLimit, stackRemaining.getCount)
        val currSlotStack = stackRemaining.split(numToPut)
        currSlot.putStack(currSlotStack)

        currSlot.onSlotChanged()
        stackRemaining
      } else {
        stackRemaining
      }
    }
    stackLeftAfterFilling
  }

  // They shift clicked a slot... do something smart!
  // Called repeatedly until it returns either an empty itemStack or a stack that doesn't match the contents of the slot clicked.
  // The return value is passed on to the
  override def transferStackInSlot(playerIn: PlayerEntity, index: Int): ItemStack = {
    val slot = this.inventorySlots.get(index)
    if(index >= numInnerSlots) { // shift clicked on a player inventory slot
      val stackInSlot = getSlot(index).getStack

      if(stackInSlot != null && containerItem.canStoreItem(containerItemStack, stackInSlot)) {
        val stackRemaining = betterMergeItemStack(stackInSlot, 0, numInnerSlots)
        slot.putStack(stackRemaining)
        slot.onSlotChanged()
        ItemStack.EMPTY //returning StackRemaining may cause an infinite loop
      } else { // Stack was empty or basket can't store that item
        ItemStack.EMPTY
      }
    } else { // shift clicked on a basket inventory slot
      if (slot != null && slot.getHasStack) {
        val stackInSlot = slot.getStack
        val stackRemaining = betterMergeItemStack(stackInSlot, numInnerSlots, this.inventorySlots.size, reverseDirection = true)
        slot.putStack(stackRemaining)
        slot.onSlotChanged()
        ItemStack.EMPTY //returning StackRemaining may cause an infinite loop
      } else {
        ItemStack.EMPTY
      }
    }
  }

  override def slotClick(slotId: Int, dragType: Int, clickTypeIn: ClickType, player: PlayerEntity): ItemStack = {
    if(clickTypeIn == ClickType.SWAP && dragType == activeSlot) {
      ItemStack.EMPTY
    } else if (clickTypeIn == ClickType.SWAP && slotId < numInnerSlots) {
      // TODO: smarter hotkey behaviour
      val basketSlot = this.inventorySlots.get(slotId)
      val basketSlotItem = if(basketSlot.getHasStack) basketSlot.getStack else ItemStack.EMPTY
      val hotbarSlot = dragType match {
        case mainbarSlot if mainbarSlot < numInnerSlots && mainbarSlot >= 0 => dragType+numInnerSlots+27
        case offhandSlot if offhandSlot == 40 => 36 + numInnerSlots
        case _ => logger.warn("Unknown swap target: " + dragType); 0
      }
      val remainder = betterMergeItemStack(basketSlotItem, hotbarSlot, hotbarSlot+1)
      basketSlot.putStack(remainder)
      basketSlot.onSlotChanged()
      ItemStack.EMPTY
    } else if (
      clickTypeIn == ClickType.PICKUP &&
        (dragType == 0 || dragType == 1) &&
        slotId.isFromUntil(0, numInnerSlots)
    ) {
      val slotpicked = this.inventorySlots.get(slotId)
      if (slotpicked != null) {
        var slotStack = slotpicked.getStack
        val heldStack = player.inventory.getItemStack
        val returnStack = if (!slotStack.isEmpty) slotStack.copy else ItemStack.EMPTY
        if (slotStack.isEmpty) {
          if (!heldStack.isEmpty && slotpicked.isItemValid(heldStack)) {
            // Holding an item and clicking on the slot.
            var numToPlace = if (dragType == 0) heldStack.getCount else 1
            if (numToPlace > slotpicked.getItemStackLimit(heldStack)) numToPlace = slotpicked.getItemStackLimit(heldStack)
            slotpicked.putStack(heldStack.split(numToPlace))
          }
        } else if (slotpicked.canTakeStack(player)) {
          // Something in the slot and the player can interact with it
          if (heldStack.isEmpty) {
            // The commented code should be entirely unreachable because we already checked for slotStack isEmpty
            //            if (slotStack.isEmpty) {
            //              // Nothing in hand, nor in the slot, so don't do anything, probably.
            //              slotpicked.putStack(ItemStack.EMPTY) // Seems unnecessary but ok
            //              player.inventory.setItemStack(ItemStack.EMPTY) // Seems unnecessary but ok
            //            } else {

            // Nothing in hand, something in the slot, so pick up some items. Limit it to one stack if left clicked, half a stack if right clicked.
            val singleStackSize = Math.min(slotStack.getCount, slotStack.getItem.getMaxStackSize): @nowarn
            val numToPickup = if (dragType == 0) singleStackSize else (singleStackSize + 1) / 2
            player.inventory.setItemStack(slotpicked.decrStackSize(numToPickup))
            if (slotStack.isEmpty) slotpicked.putStack(ItemStack.EMPTY) // Seems unnecessary but ok
            slotpicked.onTake(player, player.inventory.getItemStack)
            //            }
          } else if (slotpicked.isItemValid(heldStack)) {
            // Something in the hand and it's valid for the slot, but the slot isn't empty
            if (Container.areItemsAndTagsEqual(slotStack, heldStack)) {
              // Something in the hand and it matches what's in the slot
              var numToPlace = if (dragType == 0) heldStack.getCount else 1
              if (numToPlace + slotStack.getCount > slotpicked.getItemStackLimit(heldStack)) {
                // Too many items to shove in there
                numToPlace = slotpicked.getItemStackLimit(heldStack) - slotStack.getCount
              }
              // More stuff combined than can fit in the hand? Not relevant for placing, commenting this out
              //              if (numToPlace + slotStack.getCount > heldStack.getMaxStackSize) {
              //                numToPlace = heldStack.getMaxStackSize - slotStack.getCount
              //              }
              heldStack.shrink(numToPlace)
              slotStack.grow(numToPlace)
              slotpicked.putStack(slotStack) // Needed to update the itemstack in the nbt
            } else if (heldStack.getCount <= slotpicked.getItemStackLimit(heldStack) && slotStack.getCount <= slotStack.getMaxStackSize) {
              // Swap the 2 stacks if the slot and hand can handle them
              slotpicked.putStack(heldStack)
              player.inventory.setItemStack(slotStack)
            }
          } else if (heldStack.getMaxStackSize > 1 && Container.areItemsAndTagsEqual(slotStack, heldStack)) {
            // Holding an item that should be stackable with the slot, but the slot won't accept it. e.g. furnace output.
            // TODO: handle this case if u care
            val i3 = slotStack.getCount
            if (i3 + heldStack.getCount <= heldStack.getMaxStackSize) {
              heldStack.grow(i3)
              slotStack = slotpicked.decrStackSize(i3)
              if (slotStack.isEmpty) slotpicked.putStack(ItemStack.EMPTY)
              slotpicked.onTake(player, player.inventory.getItemStack)
            }
          }
        }
        slotpicked.onSlotChanged()
        returnStack
      } else {
        ItemStack.EMPTY
      }


    } else {
      super.slotClick(slotId, dragType, clickTypeIn, player)
    }
  }
}
