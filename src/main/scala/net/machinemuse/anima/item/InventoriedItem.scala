package net.machinemuse.anima
package item

import util.NBTTypeRef

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.container.Container
import net.minecraft.item.ItemStack
import net.minecraft.nbt._

import scala.jdk.CollectionConverters._

/**
 * Created by MachineMuse on 1/21/2021.
 */
trait InventoriedItem extends ModeChangingItem[Int] {

  def getTagCompound(stack:ItemStack): CompoundNBT = stack.getOrCreateTag()

  def canStoreItem(container: ItemStack, toStore: ItemStack): Boolean

  def accessor = IntNBTTagAccessor

  override def getValidModes(stack: ItemStack): Seq[Int] = getContents(stack).zipWithIndex.flatMap {
    case (null, _) => Seq.empty
    case (stack, _) if stack.isEmpty => Seq.empty
    case (_, index) => Seq(index)
  }

  def getSelectedStack(bag: ItemStack): ItemStack = getContentsAt(bag, getCurrentMode(bag))
  def setSelectedStack(bag: ItemStack, contents: ItemStack) = setContentsAt(bag, getCurrentMode(bag), contents)

  def getContentsAsNBTTagList(stack: ItemStack): ListNBT = getTagCompound(stack).getList("contents", NBTTypeRef.TAG_COMPOUND)

  def getContents(stack: ItemStack): Seq[ItemStack] = getContentsAsNBTTagList(stack).asScala.iterateAs[CompoundNBT].map(getFromOverloadedCompound).toSeq

  def setContents(stack: ItemStack, contents: Seq[ItemStack]): Unit = {
    val list = new ListNBT()
    contents foreach { item =>
      list.add(list.size, makeOverloadedCompound(item))
    }
    getTagCompound(stack).put("contents", list)
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

  def getNumStacks(bag: ItemStack): Int = getTagCompound(bag).getList("contents", NBTTypeRef.TAG_COMPOUND).size

  def makeOverloadedCompound(stack: ItemStack): CompoundNBT = stack.write(new CompoundNBT()).andDo(_.putInt("realcount", stack.getCount))

  // Nullsafe, returns empty, might drop a log message though
  def getFromOverloadedCompound(compound: CompoundNBT): ItemStack =
    ItemStack.read(compound).andDo { stack =>
      if(compound.contains("realcount")) {
        stack.setCount(compound.getInt("realcount"))
        compound.remove("realcount")
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
    getTagCompound(bag).put("contents", list)
  }

  def clearContents(bag: ItemStack): Unit = getTagCompound(bag).remove("contents")

  def getSize(bag: ItemStack): Int

  def getStackLimit(bag: ItemStack): Int

  def createInventory(containingItem: ItemStack): IInventory = new IInventory {

    override def isEmpty: Boolean = getNumStacks(containingItem) > 0

    override def getStackInSlot(index: Int): ItemStack = {
//      if(playerInventory.getStackInSlot(containingSlot) == containingItem) {
        getContentsAt(containingItem, index)
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
        setContentsAt(containingItem, index, stack)
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
        clearContents(containingItem)
//      } else {
//         TODO: error handling
//      }
    }

    override def getSizeInventory: Int = {
//      if(playerInventory.getStackInSlot(containingSlot) == containingItem) {
        getSize(containingItem)
//      } else {
//        0
//      }
    }

    override def getInventoryStackLimit: Int = 999
  }
}
