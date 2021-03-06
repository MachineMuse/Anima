package net.machinemuse.anima
package ghostdust

import com.google.gson.JsonObject
import com.mojang.serialization.Codec
import net.minecraft.data.IFinishedRecipe
import net.minecraft.inventory.CraftingInventory
import net.minecraft.item._
import net.minecraft.item.crafting.{ICraftingRecipe, IRecipeSerializer}
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.MathHelper
import net.minecraft.util.text.{TextFormatting, TranslationTextComponent}
import net.minecraft.world.World
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle._

import ghostdust.GhostDustingRecipe.GhostDustingIngredient
import registration.RegistryHelpers
import registration.RegistryHelpers.regSimpleItem
import util.GenCodecsByName._
import util.Logging
import util.VanillaClassEnrichers.RichItemStack
import util.VanillaCodecs.ConvenientCodec

/**
 * Created by MachineMuse on 2/2/2021.
 */
object GhostDustingRecipe extends Logging {
  @SubscribeEvent def onConstructMod(event: FMLConstructModEvent) = {}

  /*_*/
  import util.VanillaCodecs._
  private val RecipeCodec = implicitly[Codec[GhostDustingRecipe]]

  private val SERIALIZER = RegistryHelpers.regRecipeSerializer("ghost_dusting", RecipeCodec, new GhostDustingRecipe(List.empty))
  /*_*/

  case class GhostDustingIngredient(item : Item, transparency: Float, limit: Int, clamp: Boolean) extends CodecByName

  val GHOSTDUST_ITEM = regSimpleItem("ghost_dust")

  val GHOSTDUST_REMOVER_ITEM = regSimpleItem("ghost_dust_remover")


  @SubscribeEvent def onClientSetup(event: FMLClientSetupEvent) = addForgeListeners(onItemTooltip)

  def onItemTooltip(event: ItemTooltipEvent): Unit = {
    if(event.getItemStack.hasTransparency)
      event.getToolTip.add(
        new TranslationTextComponent(s"tooltip.${implicitly[MODID]}.transparency", (event.getItemStack.getTransparency * 100).toString)
          .mergeStyle(TextFormatting.GRAY, TextFormatting.ITALIC)
      )
  }
}

@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
case class GhostDustingRecipe(items: List[GhostDustingIngredient]) extends ICraftingRecipe with CodecByName with IFinishedRecipe {
  import ghostdust.GhostDustingRecipe._
  // TODO: Cleaning
  override def matches(inv: CraftingInventory, worldIn: World): Boolean = {
    var armorFound = false
    var ghostDustFound = false
    var fail = false

    val stacks = (for (i <- 0 until inv.getSizeInventory) yield inv.getStackInSlot(i)).toList.filter(!_.isEmpty)

    val ghostDustItems = items.map { _.item }
    for(stackInSlot <- stacks) {
      stackInSlot.getItem match {
        case armorItem: ArmorItem => if(!armorFound) armorFound = true else fail = true
        case ghostdust if ghostDustItems.contains(ghostdust) => ghostDustFound = true
        case _ => fail = true
      }
    }

    armorFound && ghostDustFound && !fail
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
        if(currTransparency > 0) {
          armorCopy.setTransparency(currTransparency)
        } else {
          armorCopy.removeTransparency()
        }
        armorCopy
      } else {
        ItemStack.EMPTY
      }
    } else {
      ItemStack.EMPTY
    }

  }

  val group: Option[String] = None

  override def serialize(jsonOut: JsonObject): Unit = {
    RecipeCodec.writeIntoMutableJson(this, jsonOut)
    if(getGroup.nonEmpty) {jsonOut.addProperty("group", getGroup)}
  }

  override def getAdvancementJson: JsonObject = null

  override def getAdvancementID: ResourceLocation = null

  override def canFit(width: Int, height: Int): Boolean = width * height >= 2

  override def getRecipeOutput: ItemStack = ItemStack.EMPTY

  override def getID: ResourceLocation = getId
  override def getId: ResourceLocation = new ResourceLocation(implicitly[MODID], "ghost_dusting")

  override def getSerializer: IRecipeSerializer[_] = SERIALIZER.get
}
