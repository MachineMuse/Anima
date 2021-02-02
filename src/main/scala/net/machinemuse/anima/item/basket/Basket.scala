package net.machinemuse.anima
package item
package basket

import gui.BasketContainer
import registration.AnimaRegistry.AnimaCreativeGroup
import registration.RegistryHelpers._
import util.VanillaClassEnrichers._

import net.minecraft.data.ShapedRecipeBuilder
import net.minecraft.entity.player.{PlayerEntity, ServerPlayerEntity}
import net.minecraft.item._
import net.minecraft.network.PacketBuffer
import net.minecraft.util.math.BlockRayTraceResult
import net.minecraft.util.{Unit => _, _}
import net.minecraft.world.World
import net.minecraftforge.common.IPlantable
import net.minecraftforge.event.entity.player.EntityItemPickupEvent
import net.minecraftforge.eventbus.api.Event.Result
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.{FMLConstructModEvent, GatherDataEvent}
import net.minecraftforge.fml.network.NetworkHooks
import org.apache.logging.log4j.scala.Logging

import java.util.function.Consumer
import scala.jdk.CollectionConverters._

/**
 * Created by MachineMuse on 1/21/2021.
 */

object Basket extends Logging {

  @SubscribeEvent
  def onConstructMod(event: FMLConstructModEvent) = addForgeListeners(onEntityItemPickup)

  def onEntityItemPickup(event: EntityItemPickupEvent): Unit = {
    // Iterate through the player's inventory slots for baskets
    for { slotStack <- event.getPlayer.inventory.mainInventory.asScala ++ event.getPlayer.inventory.offHandInventory.asScala} {
      if(slotStack.getItem == getInstance) {
        val remainder = getInstance.insertItem(slotStack, event.getItem.getItem)
        event.getItem.setItem(remainder)
      }
    }
    if(event.getItem.getItem.isEmpty) { // Whole stack was consumed in adding to basket
      event.setResult(Result.ALLOW) // process achievements etc. but skip adding the item to main inventory
    }
  }

  val instance = regExtendedItem("basket", () => new Basket)
  def getInstance = instance.get()

  @SubscribeEvent
  def gatherData(event: GatherDataEvent): Unit = {
    logger.debug("BasketDatagen.gatherData called")
    mkRecipeProvider(event) { consumer =>
      ShapedRecipeBuilder.shapedRecipe(getInstance)
        .patternLine(" / ")
        .patternLine("# #")
        .patternLine("###")
        .addKeyAsCriterion('/', Items.STICK)
        .addKeyAsCriterion('#', Items.SUGAR_CANE)
        .setGroup("basket")
        .buildProperly(consumer, "basket_from_sugar_cane")

      ShapedRecipeBuilder.shapedRecipe(getInstance)
        .patternLine(" / ")
        .patternLine("# #")
        .patternLine("###")
        .addKeyAsCriterion('/', Items.STICK)
        .addKeyAsCriterion('#', Items.BAMBOO)
        .setGroup("basket")
        .buildProperly(consumer, "basket_from_bamboo")
    }
  }

}

// counterintuitively, this will autosubscribe all the methods annotated with @SubscribeEvent in the companion object above.
@Mod.EventBusSubscriber(modid = Anima.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
class Basket extends Item(new Item.Properties().maxStackSize(1).group(AnimaCreativeGroup).setISTER(() => () => BasketISTER)) with InventoriedItem with Logging {
  def isVeggie(stack: ItemStack): Boolean = stack.getItem.isFood && !stack.getItem.getFood.isMeat
  def isPlantable(stack: ItemStack): Boolean = OptionCast[BlockItem](stack.getItem).exists(_.getBlock.isInstanceOf[IPlantable])

  override def canStoreItem(container: ItemStack, toStore: ItemStack): Boolean = isVeggie(toStore) || isPlantable(toStore)

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

  def tryOpenGuiServerSide(hand: Hand)(player: ServerPlayerEntity): Unit = {
    val containerProvider = mkContainerProvider("basket", (id,inv,player) => new BasketContainer(id,inv,hand))
    val consumer: Consumer[PacketBuffer] = (t: PacketBuffer) => { t.writeByte(hand.ordinal()) }
    NetworkHooks.openGui(player, containerProvider, consumer)
  }
  // Called when the item is in the player's hand and right clicked. Replaces the item with the return value.
  override def onItemRightClick(world: World, player: PlayerEntity, hand: Hand): ActionResult[ItemStack] = {
    if(player.isSneaking) {
      if (!world.isRemote) {
        logger.debug("item used facing elsewhere while sneaking by a player on the server side")
        // Check if the player object is a ServerPlayerEntity, should be but just to be sure
        player.optionallyDoAs [ServerPlayerEntity] (tryOpenGuiServerSide(hand))
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

  override def getDefaultMode(stack: ItemStack): Int = 0
}