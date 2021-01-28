package net.machinemuse.anima.item

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.container.Container
import net.minecraft.item.ItemStack
import net.minecraft.nbt.{CompoundNBT, INBT, ListNBT}

/**
 * Created by MachineMuse on 1/21/2021.
 */
trait InventoriedItem {

  def getTagCompound(stack:ItemStack): CompoundNBT = stack.getOrCreateTag()

  def canStoreItem(container: ItemStack, toStore: ItemStack): Boolean

  def getSelectedStack(bag: ItemStack): ItemStack = {
    getContentsAt(bag, getSelectedSlot(bag))
  }
  def setSelectedStack(bag: ItemStack, contents: ItemStack) = {
    setContentsAt(bag, getSelectedSlot(bag), contents)
  }

  def getSelectedSlot(stack: ItemStack): Int = {
    val tag = getTagCompound(stack)
    if (tag.contains("selected")) tag.getInt("selected")
    else {
      tag.putInt("selected", 0)
      0
    }
  }

  def setSelectedSlot(stack:ItemStack, i:Int): Unit = {
    val tag = getTagCompound(stack)
    tag.putInt("selected", i)
  }

  // Nullsafe, returns a new list
  def getContentsAsNBTTagList(stack: ItemStack): ListNBT = {
    val tag = getTagCompound(stack)
    if (tag.contains("contents")) tag.getList("contents", 10)
    else {
      val list = new ListNBT()
      tag.put("contents", list)
      list
    }
  }

  def getContents(stack: ItemStack): Seq[ItemStack] = {
    val list = getContentsAsNBTTagList(stack)
    for (i <- 0 until list.size) yield {
      getFromOverloadedCompound(list.getCompound(i))
    }
  }

  def setContents(stack: ItemStack, contents: Seq[ItemStack]): Unit = {
    val list = new ListNBT()
    contents foreach { item =>
      list.add(list.size, makeOverloadedCompound(item))
    }
    getTagCompound(stack).put("contents", list)
  }

  def isValidForSlot(n: Int, bag: ItemStack, stackToInsert: ItemStack): Boolean

  // returns remainder
  def insertItem(bag:ItemStack, stackToInsert:ItemStack): ItemStack = {
    val contents = getContents(bag)

    val stackLeftAfterMatching = contents.indices.foldLeft(stackToInsert) { (stackRemaining, i) =>
      val currSlotStack = contents(i)
      if(currSlotStack != null && Container.areItemsAndTagsEqual(stackRemaining, currSlotStack) && isValidForSlot(i, bag, stackRemaining)) {
        val numToPut = Math.min(getStackLimit(bag) - currSlotStack.getCount, stackRemaining.getCount)
        currSlotStack.grow(numToPut)
        stackRemaining.shrink(numToPut)
        setContentsAt(bag, i, currSlotStack)
        stackRemaining
      } else {
        stackRemaining
      }
    }
    val stackLeftAfterFilling = (0 until getSize(bag)).foldLeft(stackLeftAfterMatching) {(stackRemaining, i) =>
      val currSlotStack = if(i < contents.size) contents(i) else ItemStack.EMPTY
      if((currSlotStack == null || currSlotStack.isEmpty) && isValidForSlot(i, bag, stackRemaining)) {
        val numToPut = Math.min(getStackLimit(bag), stackRemaining.getCount)
        val currSlotStack = stackRemaining.split(numToPut)
        setContentsAt(bag, i, currSlotStack)
        stackRemaining
      } else {
        stackRemaining
      }
    }
    stackLeftAfterFilling
  }

  def getNumStacks(bag:ItemStack): Int = {
    val tag = getTagCompound(bag)
    if (tag.contains("contents")) tag.getList("contents", 10).size else 0
  }

  def makeOverloadedCompound(stack: ItemStack): CompoundNBT = {
    val realcount = stack.getCount
    val compound = stack.write(new CompoundNBT())
    compound.putInt("realcount", realcount)
    compound
  }

  // Nullsafe, returns empty, might drop a log message though
  def getFromOverloadedCompound(compound: CompoundNBT): ItemStack = {
    val stack = ItemStack.read(compound) // Nullsafe, returns empty
    val realCount = if(compound.contains("realcount")) compound.getInt("realcount") else stack.getCount
    stack.setCount(realCount)
    stack
  }

  // Nullsafe, returns empty
  def getContentsAt(bag: ItemStack, i: Int): ItemStack = {
    val list = getContentsAsNBTTagList(bag)
    getFromOverloadedCompound(list.getCompound(i)) // Index-safe, returns a new compound
  }

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

  def clearContents(bag: ItemStack): Unit = {
    getTagCompound(bag).remove("contents")
  }

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
