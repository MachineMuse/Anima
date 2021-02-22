package net.machinemuse.anima
package item

import net.minecraft.inventory.container.Container
import net.minecraft.item.ItemStack
import net.minecraft.nbt._
import net.minecraft.util.text.{ITextComponent, TextFormatting}
import net.minecraftforge.items.{IItemHandler, IItemHandlerModifiable}

import java.util
import scala.jdk.CollectionConverters._

import constants.NBTTypeRef

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

  def getStackInSelectedSlot(bag: ItemStack): ItemStack = getContentsAt(bag, getCurrentMode(bag))
  def setStackInSelectedSlot(bag: ItemStack, contents: ItemStack) = setContentsAt(bag, getCurrentMode(bag), contents)

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

  def createInventory(container: ItemStack): IItemHandler = new IItemHandlerModifiable {

    override def getSlots: Int = getSize(container)

    override def getStackInSlot(index: Int): ItemStack = {
        getContentsAt(container, index)
    }

    override def insertItem(slot: Int, stack: ItemStack, simulate: Boolean): ItemStack = {
      val existingItem = getStackInSlot(slot)
      if(isItemValid(slot, stack) && (existingItem.isEmpty || (existingItem.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(existingItem, stack)))) {
        val numInserted = Math.min(stack.getCount, getSlotLimit(slot) - existingItem.getCount)
        val result = stack.copy()
        result.setCount(stack.getCount - numInserted)
        if(!simulate) {
          existingItem.setCount(existingItem.getCount + numInserted)
          setContentsAt(container, slot, existingItem)
        }
        result
      } else {
        stack
      }
    }

    override def extractItem(slot: Int, amount: Int, simulate: Boolean): ItemStack = {
      val existingItem = getStackInSlot(slot)
      val numToExtract = Seq(amount, existingItem.getCount, existingItem.getMaxStackSize).min
      val result = existingItem.copy()
      result.setCount(numToExtract)
      if(!simulate) {
        existingItem.shrink(numToExtract)
        setContentsAt(container, slot, existingItem)
      }
      result
    }

    override def getSlotLimit(slot: Int): Int = getStackLimit(container)

    override def isItemValid(slot: Int, stack: ItemStack): Boolean = isValidForSlot(slot, container, stack)

    override def setStackInSlot(slot: Int, stack: ItemStack): Unit = {
      setContentsAt(container, slot, stack)
    }
  }
}
