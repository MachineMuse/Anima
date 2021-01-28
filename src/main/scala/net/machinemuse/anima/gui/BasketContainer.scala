package net.machinemuse.anima.gui

import net.machinemuse.anima.item.LockedSlot
import net.machinemuse.anima.item.basket.Basket
import net.machinemuse.anima.registration.AnimaRegistry
import net.minecraft.entity.player.{PlayerEntity, PlayerInventory}
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.container.{ClickType, Container, Slot}
import net.minecraft.item.ItemStack
import org.apache.logging.log4j.LogManager

import scala.annotation.nowarn
import scala.jdk.CollectionConverters._

/**
 * Created by MachineMuse on 1/22/2021.
 */
class BasketContainer(windowID: Int, playerInventory: PlayerInventory) extends Container(AnimaRegistry.BASKET_CONTAINER.get(), windowID) {
  override def canInteractWith(playerIn: PlayerEntity): Boolean = true

  // Directly reference a log4j logger.
  private val LOGGER = LogManager.getLogger

  val basketSlotNumber: Int = {
    val currentItem = playerInventory.getCurrentItem
    val offhandItem = playerInventory.offHandInventory.get(0)
    // First, check if the main hand item is a basket
    if(currentItem != null && currentItem.getItem.isInstanceOf[Basket]) {
      playerInventory.currentItem
      //otherwise, check if the offhand item is a basket
    } else if(offhandItem != null && offhandItem.getItem.isInstanceOf[Basket]) {
      40
    } else {
      LOGGER.warn("Couldn't find slot number of basket")
      -1
    }
  }
  val selectedBasket: ItemStack = playerInventory.getStackInSlot(basketSlotNumber)

  val basketItem: Basket = AnimaRegistry.BASKET_ITEM.get

  val basketInventory: IInventory = basketItem.createInventory(selectedBasket)

  class BasketSlot(inventoryIn: IInventory, index: Int, xPosition: Int, yPosition: Int) extends Slot(inventoryIn, index, xPosition, yPosition) {
    override def isItemValid(stack: ItemStack): Boolean = basketItem.canStoreItem(selectedBasket, stack)

  }

  for (i <- 0 until 3) {
    for (j <- 0 until 3) {
      this.addSlot(new BasketSlot(basketInventory, j + i * 3, 62 + j * 18, 17 + i * 18))
    }
  }

  for (k <- 0 until 3) {
    for (i1 <- 0 until 9) {
      this.addSlot(new Slot(playerInventory, i1 + k * 9 + 9, 8 + i1 * 18, 84 + k * 18))
    }
  }

  for (l <- 0 until 9) {
    if(l == basketSlotNumber) {
      this.addSlot(new LockedSlot(playerInventory, l, 8 + l * 18, 142))
    } else {
      this.addSlot(new Slot(playerInventory, l, 8 + l * 18, 142))
    }
  }

  if(basketSlotNumber == 40) {
    this.addSlot(new LockedSlot(playerInventory, 40, -18, 142))
  } else {
    this.addSlot(new Slot(playerInventory, 40, -18, 142))
  }


  // They shift clicked a slot... do something smart!
  // Called repeatedly until it returns either an empty itemStack or a stack that doesn't match the contents of the slot clicked.
  // The return value is passed on to the
  override def transferStackInSlot(playerIn: PlayerEntity, index: Int): ItemStack = {
    val slot = this.inventorySlots.get(index)
    if(index >= 9) { // shift clicked on a player inventory slot
      val stackInSlot = getSlot(index).getStack
      val returnStack = stackInSlot.copy()

      if(stackInSlot != null && basketItem.canStoreItem(selectedBasket, stackInSlot)) {
        val stackRemaining = betterMergeItemStack(stackInSlot, 0, 9)
        slot.putStack(stackRemaining)
        slot.onSlotChanged()
        ItemStack.EMPTY //returning StackRemaining may cause an infinite loop
      } else { // Stack was empty or basket can't store that item
        ItemStack.EMPTY
      }
    } else { // shift clicked on a basket inventory slot
      if (slot != null && slot.getHasStack) {
        val stackInSlot = slot.getStack
        val returnStack = stackInSlot.copy()
        val stackRemaining = betterMergeItemStack(stackInSlot, 9, this.inventorySlots.size, true)
        slot.putStack(stackRemaining)
        slot.onSlotChanged()
        ItemStack.EMPTY //returning StackRemaining may cause an infinite loop
      } else {
        ItemStack.EMPTY
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

  override def slotClick(slotId: Int, dragType: Int, clickTypeIn: ClickType, player: PlayerEntity): ItemStack = {
    if(clickTypeIn == ClickType.SWAP && dragType == basketSlotNumber) {
      ItemStack.EMPTY
    } else if (clickTypeIn == ClickType.SWAP && slotId < 9) {
      // TODO: smarter hotkey behaviour
      val basketSlot = this.inventorySlots.get(slotId)
      val basketSlotItem = if(basketSlot.getHasStack) basketSlot.getStack else ItemStack.EMPTY
      val hotbarSlot = dragType match {
        case mainbarSlot if mainbarSlot < 9 && mainbarSlot >= 0 => dragType+9+27
        case offhandSlot if offhandSlot == 40 => 36 + 9
        case _ => LOGGER.warn("Unknown swap target: " + dragType); 0
      }
      val remainder = betterMergeItemStack(basketSlotItem, hotbarSlot, hotbarSlot+1)
      basketSlot.putStack(remainder)
      basketSlot.onSlotChanged()
      ItemStack.EMPTY
    } else if (
      clickTypeIn == ClickType.PICKUP &&
        (dragType == 0 || dragType == 1) &&
        slotId < 9 && slotId >= 0
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
