package net.machinemuse.anima
package item

import registration.AnimaRegistry
import registration.RegistryHelpers.RECIPE_SERIALIZERS
import util.VanillaClassEnrichers.RichItemStack

import com.google.gson.JsonObject
import net.minecraft.inventory.CraftingInventory
import net.minecraft.item.crafting.{ICraftingRecipe, IRecipeSerializer}
import net.minecraft.item.{ArmorItem, ItemStack}
import net.minecraft.network.PacketBuffer
import net.minecraft.util.ResourceLocation
import net.minecraft.util.text.TranslationTextComponent
import net.minecraft.world.World
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.RegistryObject
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent
import net.minecraftforge.registries.ForgeRegistryEntry
import org.apache.logging.log4j.scala.Logging

/**
 * Created by MachineMuse on 2/2/2021.
 */
object GhostDustingRecipe extends Logging {
  val SERIALIZER: RegistryObject[GhostDustingRecipeSerializer] = RECIPE_SERIALIZERS.register("ghost_dusting", () => new GhostDustingRecipeSerializer)

  def getSerializer = SERIALIZER.get

  @SubscribeEvent def onConstructMod(event: FMLConstructModEvent) = addForgeListeners(onItemTooltip)

  class GhostDustingRecipeSerializer extends ForgeRegistryEntry[IRecipeSerializer[_]] with IRecipeSerializer[GhostDustingRecipe] {
    override def read(recipeId: ResourceLocation, json: JsonObject): GhostDustingRecipe = new GhostDustingRecipe
    override def read(recipeId: ResourceLocation, buffer: PacketBuffer): GhostDustingRecipe = new GhostDustingRecipe
    override def write(buffer: PacketBuffer, recipe: GhostDustingRecipe): Unit = { }
  }


  def onItemTooltip(event: ItemTooltipEvent): Unit = {
    val stack = event.getItemStack
    if(stack.hasTransparency) {
      event.getToolTip.add(new TranslationTextComponent(s"tooltip.${Anima.MODID}.transparency", (stack.getTransparency * 100).toString))
    }
  }
}

@Mod.EventBusSubscriber(modid = Anima.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
class GhostDustingRecipe extends ICraftingRecipe with Logging {
  override def matches(inv: CraftingInventory, worldIn: World): Boolean = {
    var armorFound = false
    var ghostDustFound = false
    var tooMany = false
    for(slot <- 0 until inv.getSizeInventory) {
      val stackInSlot = inv.getStackInSlot(slot)
      stackInSlot.getItem match {
        case armorItem: ArmorItem => if(!armorFound) armorFound = true else tooMany = true
        case ghostdust if ghostdust == AnimaRegistry.GHOSTDUST_ITEM.get() => ghostDustFound = true
        case ghostdustremover if ghostdustremover == AnimaRegistry.GHOSTDUST_REMOVER_ITEM.get() => ghostDustFound = true
        case _ =>
      }
    }

    armorFound && ghostDustFound && !tooMany
  }

  override def getCraftingResult(inv: CraftingInventory): ItemStack = {
    var armorFound = ItemStack.EMPTY
    var ghostDustFound = 0
    var removerFound = false
    var tooManyArmors = false
    var tooManyRemovers = false
    for(slot <- 0 until inv.getSizeInventory) {
      val stackInSlot = inv.getStackInSlot(slot)
      stackInSlot.getItem match {
        case armorItem: ArmorItem => if(armorFound.isEmpty) armorFound = stackInSlot else tooManyArmors = true
        case ghostdust if ghostdust == AnimaRegistry.GHOSTDUST_ITEM.get() => ghostDustFound += 1
        case ghostdustremover if ghostdustremover == AnimaRegistry.GHOSTDUST_REMOVER_ITEM.get() => if(removerFound) tooManyRemovers = true else removerFound = true
        case _ =>
      }
    }
    val transparencyPerDust = 0.125F
    if( !armorFound.isEmpty &&
        !tooManyArmors &&
        !tooManyRemovers &&
        (ghostDustFound > 0 || removerFound)
    ) {
      val armorCopy = armorFound.copy()

      if(removerFound) armorCopy.removeTransparency()

      if(ghostDustFound > 0) {
        if((transparencyPerDust * ghostDustFound + armorCopy.getTransparency) <= 1.0F) {
          armorCopy.setTransparency(armorCopy.getTransparency + transparencyPerDust * ghostDustFound)
          armorCopy
        } else {
          ItemStack.EMPTY
        }
      } else {
        armorCopy
      }
    } else {
      ItemStack.EMPTY
    }

  }

  override def canFit(width: Int, height: Int): Boolean = width * height >= 2

  override def getRecipeOutput: ItemStack = ItemStack.EMPTY

  override def getId: ResourceLocation = new ResourceLocation(Anima.MODID, "ghost_dusting")

  override def getSerializer: IRecipeSerializer[GhostDustingRecipe] = getSerializer
}
