package net.machinemuse.anima
package item

import util.NBTTypeRef

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.container.Container
import net.minecraft.item.ItemStack
import net.minecraft.nbt._
import net.minecraft.util.text.{ITextComponent, TextFormatting}

import java.util
import scala.jdk.CollectionConverters._

/**
 * Created by MachineMuse on 1/21/2021.
 */
trait InventoriedItem extends ModeChangingItem[Int] {

  // TODO: use ItemStackHelper methods where applicable

  def addContentsToTooltip(bag: ItemStack, tooltip: util.List[ITextComponent]) = {
    for (stack <- getContents(bag)) {
      if(stack != null && !stack.isEmpty) {
        val iformattabletextcomponent = stack.getDisplayName.deepCopy
        iformattabletextcomponent.mergeStyle(TextFormatting.GRAY, TextFormatting.ITALIC).appendString(" x").appendString(String.valueOf(stack.getCount))
        tooltip.add(iformattabletextcomponent)
      }
    }

  }
  def canStoreItem(container: ItemStack, toStore: ItemStack): Boolean

  def accessor = IntNBTTagAccessor

  override def getValidModes(stack: ItemStack): Seq[Int] = getContents(stack).zipWithIndex.flatMap {
    case (null, _) => Seq.empty
    case (stack, _) if stack.isEmpty => Seq.empty
    case (_, index) => Seq(index)
  }

  def getSelectedStack(bag: ItemStack): ItemStack = getContentsAt(bag, getCurrentMode(bag))
  def setSelectedStack(bag: ItemStack, contents: ItemStack) = setContentsAt(bag, getCurrentMode(bag), contents)

  private def getContentsAsNBTTagList(bag: ItemStack): ListNBT = bag.getOrCreateTag().getList("contents", NBTTypeRef.TAG_COMPOUND)

  def getContents(bag: ItemStack): Seq[ItemStack] = getContentsAsNBTTagList(bag).asScala.iterateAs[CompoundNBT].map(getFromOverloadedCompound).toSeq

  def setContents(bag: ItemStack, contents: Seq[ItemStack]): Unit = {
    val list = new ListNBT()
    contents foreach { item =>
      list.add(list.size, makeOverloadedCompound(item))
    }
    bag.getOrCreateTag().put("contents", list)
  }

  def isValidForSlot(n: Int, bag: ItemStack, stackToInsert: ItemStack): Boolean

  private def transferInStacks(from: ItemStack, to: ItemStack, num: Int) = {
    from.shrink(num)
    to.grow(num)
  }

  // returns remainder
  def insertItem(bag: ItemStack, stackToInsert: ItemStack): ItemStack = {
    val contents = getContents(bag)

    // First, try to fill existing stacks of the same type
    val stackLeftAfterMatching = contents.indices.foldLeft(stackToInsert) { (stackRemaining, i) =>
      val currSlotStack = contents(i)

      if(currSlotStack != null && Container.areItemsAndTagsEqual(stackRemaining, currSlotStack) && isValidForSlot(i, bag, stackRemaining)) {
        val numToPut = Math.min(getStackLimit(bag) - currSlotStack.getCount, stackRemaining.getCount)
        transferInStacks(stackRemaining, currSlotStack, numToPut)
        setContentsAt(bag, i, currSlotStack)
      }
      stackRemaining
    }

    // Then, try to fill empty slots
    val stackLeftAfterFilling = (0 until getSize(bag)).foldLeft(stackLeftAfterMatching) {(stackRemaining, i) =>
      val currSlotStack = if(i < contents.size) contents(i) else ItemStack.EMPTY
      if((currSlotStack == null || currSlotStack.isEmpty) && isValidForSlot(i, bag, stackRemaining)) {
        val numToPut = Math.min(getStackLimit(bag), stackRemaining.getCount)
        val currSlotStack = stackRemaining.split(numToPut)
        setContentsAt(bag, i, currSlotStack)
      }
      stackRemaining
    }

    // Anything left over couldn't be inserted.
    stackLeftAfterFilling

  }

  def getNumStacks(bag: ItemStack): Int = bag.getOrCreateTag().getList("contents", NBTTypeRef.TAG_COMPOUND).size

  def makeOverloadedCompound(stack: ItemStack): CompoundNBT = stack.write(new CompoundNBT()).andDo(_.putInt("realcount", stack.getCount))

  // Nullsafe, returns empty, might drop a log message though
  def getFromOverloadedCompound(compound: CompoundNBT): ItemStack =
    ItemStack.read(compound.copy()).andDo { stack =>
      if(compound.contains("realcount")) {
        stack.setCount(compound.getInt("realcount"))
        if(stack.hasTag) {
          stack.getTag.remove("realcount")
        }
      }
    }

  // Nullsafe, returns empty
  def getContentsAt(bag: ItemStack, i: Int): ItemStack = getFromOverloadedCompound(getContentsAsNBTTagList(bag).getCompound(i))

  def setContentsAt(bag: ItemStack, i: Int, stackToInsert: ItemStack): INBT = {
    val list = getContentsAsNBTTagList(bag)
    if(list.size <= i) {
      for(j <- list.size to i) {
        list.add(j, new CompoundNBT())
      }
    }
    list.set(i, makeOverloadedCompound(stackToInsert))
    bag.getOrCreateTag().put("contents", list)
  }

  def clearContents(bag: ItemStack): Unit = bag.getOrCreateTag().remove("contents")

  def getSize(bag: ItemStack): Int

  def getStackLimit(bag: ItemStack): Int

  def createInventory(container: ItemStack): IInventory = new IInventory {

    override def isEmpty: Boolean = getNumStacks(container) > 0

    override def getStackInSlot(index: Int): ItemStack = {
//      if(playerInventory.getStackInSlot(containingSlot) == containingItem) {
        getContentsAt(container, index)
//      } else {
//        ItemStack.EMPTY
//      }
    }

    override def decrStackSize(index: Int, requestedCount: Int): ItemStack = {
//      if(playerInventory.getStackInSlot(containingSlot) == containingItem) {
        val contents = getStackInSlot(index)
        val actualCount = Math.min(requestedCount, contents.getCount)
        val extracted = new ItemStack(contents.getItem, actualCount)
        contents.setCount(contents.getCount - actualCount)
        setInventorySlotContents(index, contents)
        extracted
//      } else {
//        ItemStack.EMPTY
//      }
    }

    override def removeStackFromSlot(index: Int): ItemStack = {
//      if(playerInventory.getStackInSlot(containingSlot) == containingItem) {
        val contents = getStackInSlot(index)
        setInventorySlotContents(index, null)
        contents
//      } else {
//        ItemStack.EMPTY
//      }
    }

    override def setInventorySlotContents(index: Int, stack: ItemStack): Unit = {
//      if(playerInventory.getStackInSlot(containingSlot) == containingItem) {
        setContentsAt(container, index, stack)
//      } else {
//         todo: error handling
//      }
    }

    override def markDirty(): Unit = {} // not needed since we update instantly

    override def isUsableByPlayer(player: PlayerEntity): Boolean = {
      true
//      playerInventory.getStackInSlot(containingSlot) == containingItem
    }

    override def clear(): Unit = {
//      if(playerInventory.getStackInSlot(containingSlot) == containingItem) {
        clearContents(container)
//      } else {
//         TODO: error handling
//      }
    }

    override def getSizeInventory: Int = {
//      if(playerInventory.getStackInSlot(containingSlot) == containingItem) {
        getSize(container)
//      } else {
//        0
//      }
    }

    override def getInventoryStackLimit: Int = getStackLimit(container)
  }
}
