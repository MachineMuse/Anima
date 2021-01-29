package net.machinemuse.anima.item.basket

import net.machinemuse.anima.gui.BasketContainer
import net.machinemuse.anima.item.InventoriedItem
import net.machinemuse.anima.registration.AnimaRegistry.AnimaCreativeGroup
import net.machinemuse.anima.util.OptionCast
import net.machinemuse.anima.util.OptionCast.Optionally
import net.machinemuse.anima.util.VanillaClassEnrichers._
import net.minecraft.entity.player.{PlayerEntity, ServerPlayerEntity}
import net.minecraft.item._
import net.minecraft.nbt.CompoundNBT
import net.minecraft.network.PacketBuffer
import net.minecraft.util.math.BlockRayTraceResult
import net.minecraft.util.{ActionResult, ActionResultType, Hand}
import net.minecraft.world.World
import net.minecraftforge.common.IPlantable
import net.minecraftforge.fml.network.NetworkHooks
import org.apache.logging.log4j.LogManager

/**
 * Created by MachineMuse on 1/21/2021.
 */
class Basket extends Item(new Item.Properties().maxStackSize(1).group(AnimaCreativeGroup).setISTER(() => () => BasketISTER)) with InventoriedItem {
  def isVeggie(stack: ItemStack): Boolean = stack.getItem.isFood && !stack.getItem.getFood.isMeat
  def isPlantable(stack: ItemStack): Boolean = OptionCast[BlockItem](stack.getItem).fold(false)(_.getBlock.isInstanceOf[IPlantable])


  override def canStoreItem(container: ItemStack, toStore: ItemStack): Boolean = isVeggie(toStore) || isPlantable(toStore)

  override def getTagCompound(stack: ItemStack): CompoundNBT = super.getTagCompound(stack)

  // Directly reference a log4j logger.
  private val LOGGER = LogManager.getLogger

  // Called when the item is used on a block
  override def onItemUse(context: ItemUseContext): ActionResultType = {
    val player = context.getPlayer
    if(player.isSneaking) {
      ActionResultType.PASS // Passes to onItemRightClick
    } else {
      val bag = context.getItem
      val selectedSlotStack = getSelectedStack(bag)
      if(!selectedSlotStack.isEmpty){
        val innerContext = new ItemUseContext(player, context.getHand, new BlockRayTraceResult(context.getHitVec,context.getFace, context.getPos, context.isInside))
        deferItemUseT(player, context.getHand, _.onItemUse(innerContext))
      } else {
        ActionResultType.PASS
      }
    }
  }

  def tryOpenGuiServerSide(player: ServerPlayerEntity): Unit = {
    val containerProvider = mkContainerProvider("basket", (id,inv,player) => new BasketContainer(id,inv))
    val consumer = (t: PacketBuffer) => { }
    NetworkHooks.openGui(player, containerProvider, consumer(_))
  }
  // Called when the item is in the player's hand and right clicked. Replaces the item with the return value.
  override def onItemRightClick(world: World, player: PlayerEntity, hand: Hand): ActionResult[ItemStack] = {
    if(player.isSneaking) {
      if (!world.isRemote) {
        LOGGER.info("item used facing elsewhere while sneaking by a player on the server side")
        // Check if the player object is a ServerPlayerEntity, should be but just to be sure
        player.optionallyDoAs [ServerPlayerEntity] (tryOpenGuiServerSide)
      }
      ActionResult.resultConsume(player.itemInHand(hand))
    } else {
      // player not sneaking
      val bag = player.getHeldItem(hand)
      val selectedSlotStack = getSelectedStack(bag)
      if(!selectedSlotStack.isEmpty){
        deferItemUse(player, hand, _.useItemRightClick(world, player, hand))
      } else {
        ActionResult.resultPass(bag)
      }
    }
  }

  def deferItemUseT(player: PlayerEntity, hand: Hand, f: ItemStack => ActionResultType): ActionResultType = {
    deferItemUse(player, hand, stack => new ActionResult(f(stack), stack)).getType
  }

  // Defers the given action to the selected item from the item inventory in question
  def deferItemUse(player: PlayerEntity, hand: Hand, useItem: ItemStack => ActionResult[ItemStack]): ActionResult[ItemStack] = {
    val (inventory, slot) = hand match {
      case Hand.MAIN_HAND => (player.inventory.mainInventory, player.inventory.currentItem)
      case Hand.OFF_HAND => (player.inventory.offHandInventory, 0)
    }
    val bag = inventory.get(slot)
    val selectedStack = getSelectedStack(bag)

    inventory.set(slot, selectedStack)
    val usedStackResult = useItem(selectedStack)
    setSelectedStack(bag, usedStackResult.getResult)
    inventory.set(slot, bag)
    new ActionResult[ItemStack](usedStackResult.getType, bag)
  }

  // TODO: dynamic or somethin
  override def getSize(stack: ItemStack): Int = 9

  // TODO: dynamic or somethin
  override def isValidForSlot(n: Int, bag: ItemStack, stackToInsert: ItemStack): Boolean = canStoreItem(bag, stackToInsert)

  // TODO: dynamic or somethin
  override def getStackLimit(bag: ItemStack): Int = 999
}
