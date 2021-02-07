package net.machinemuse.anima
package ghostdust

import ghostdust.GhostDustingRecipe.{GhostDustingIngredient, SERIALIZER}
import registration.RegistryHelpers.RECIPE_SERIALIZERS
import util.VanillaClassEnrichers.RichItemStack

import com.mojang.serialization.Codec
import net.minecraft.inventory.CraftingInventory
import net.minecraft.item._
import net.minecraft.item.crafting.{ICraftingRecipe, IRecipeSerializer}
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.MathHelper
import net.minecraft.util.text.{TextFormatting, TranslationTextComponent}
import net.minecraft.world.World
import net.minecraftforge.api.distmarker.{Dist, OnlyIn}
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.RegistryObject
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.{FMLClientSetupEvent, FMLConstructModEvent}
import org.apache.logging.log4j.scala.Logging

/**
 * Created by MachineMuse on 2/2/2021.
 */
object GhostDustingRecipe extends Logging {
  @SubscribeEvent def onConstructMod(event: FMLConstructModEvent) = {}

  /*_*/
  import util.VanillaCodecs._
  val RecipeCodec = implicitly[Codec[GhostDustingRecipe]]
  private val SERIALIZER: RegistryObject[IRecipeSerializer[GhostDustingRecipe]] = RECIPE_SERIALIZERS.register("ghost_dusting", () => RecipeCodec.mkSerializer(new GhostDustingRecipe(List.empty)))
  /*_*/

  case class GhostDustingIngredient(item : Item, transparency: Float, limit: Int, clamp: Boolean)

  @OnlyIn(Dist.CLIENT) @SubscribeEvent def onClientSetup(event: FMLClientSetupEvent) = addForgeListeners(onItemTooltip)

  @OnlyIn(Dist.CLIENT) def onItemTooltip(event: ItemTooltipEvent): Unit = {
    if(event.getItemStack.hasTransparency)
      event.getToolTip.add(
        new TranslationTextComponent(s"tooltip.${Anima.MODID}.transparency", (event.getItemStack.getTransparency * 100).toString)
          .mergeStyle(TextFormatting.GRAY, TextFormatting.ITALIC)
      )
  }
}

@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
case class GhostDustingRecipe(items: List[GhostDustingIngredient]) extends ICraftingRecipe with Logging {
  // TODO: Cleaning
  override def matches(inv: CraftingInventory, worldIn: World): Boolean = {
    var armorFound = false
    var ghostDustFound = false
    var tooMany = false

    val ghostDustItems = items.map { _.item }
    for(slot <- 0 until inv.getSizeInventory) {
      val stackInSlot = inv.getStackInSlot(slot)
      stackInSlot.getItem match {
        case armorItem: ArmorItem => if(!armorFound) armorFound = true else tooMany = true
        case ghostdust if ghostDustItems.contains(ghostdust) => ghostDustFound = true
        case _ =>
      }
    }

    armorFound && ghostDustFound && !tooMany
  }

  override def getCraftingResult(inv: CraftingInventory): ItemStack = {
    var armorFound = ItemStack.EMPTY
    var tooManyArmors = false
    var ghostDustFound = 0

    val ghostDustItems = items.map { _.item }

    for(slot <- 0 until inv.getSizeInventory) {
      val stackInSlot = inv.getStackInSlot(slot)
      stackInSlot.getItem match {
        case armorItem: ArmorItem => if(armorFound.isEmpty) armorFound = stackInSlot else tooManyArmors = true
        case ghostdust if ghostDustItems.contains(ghostdust) => ghostDustFound += 1
        case _ =>
      }
    }
    val transparencyPerDust = 0.125F
    if( !armorFound.isEmpty &&
        !tooManyArmors &&
        (ghostDustFound > 0)
    ) {
      var fail = false
      val armorCopy = armorFound.copy()
      var currTransparency = armorCopy.getTransparency
      for(ingredient <- items) {
        if(!fail) {
          val item = ingredient.item
          var found = 0
          for(slot <- 0 until inv.getSizeInventory) {
            if(inv.getStackInSlot(slot).getItem == item) found += 1
          }
          if(ingredient.limit > 0 && found > ingredient.limit) {
            fail = true
          } else {
            val newTransparency = found * ingredient.transparency + currTransparency
            if(newTransparency.isFromTo(0.0F, 1.0F)) {
              currTransparency = newTransparency
            } else {
              if(ingredient.clamp) currTransparency = MathHelper.clamp(newTransparency, 0.0F, 1.0F) else fail = true
            }
          }
        }
      }
      if(!fail) {
        armorCopy.setTransparency(currTransparency)
        armorCopy
      } else {
        ItemStack.EMPTY
      }
    } else {
      ItemStack.EMPTY
    }

  }

  override def canFit(width: Int, height: Int): Boolean = width * height >= 2

  override def getRecipeOutput: ItemStack = ItemStack.EMPTY

  override def getId: ResourceLocation = new ResourceLocation(Anima.MODID, "ghost_dusting")

  override def getSerializer: IRecipeSerializer[GhostDustingRecipe] = SERIALIZER.get
}
