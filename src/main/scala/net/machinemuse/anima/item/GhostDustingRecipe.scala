package net.machinemuse.anima
package item

import item.GhostDustingRecipe.GhostDustingIngredient
import registration.AnimaRegistry
import registration.RegistryHelpers.RECIPE_SERIALIZERS
import util.VanillaClassEnrichers.RichItemStack

import com.google.gson.{Gson, JsonObject}
import net.minecraft.inventory.CraftingInventory
import net.minecraft.item.crafting.{ICraftingRecipe, IRecipeSerializer}
import net.minecraft.item.{ArmorItem, ItemStack}
import net.minecraft.network.PacketBuffer
import net.minecraft.util.ResourceLocation
import net.minecraft.util.text.{TextFormatting, TranslationTextComponent}
import net.minecraft.world.World
import net.minecraftforge.api.distmarker.{Dist, OnlyIn}
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.RegistryObject
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.{FMLClientSetupEvent, FMLConstructModEvent}
import net.minecraftforge.registries.ForgeRegistryEntry
import org.apache.logging.log4j.scala.Logging

import scala.jdk.CollectionConverters.IteratorHasAsScala

/**
 * Created by MachineMuse on 2/2/2021.
 */
object GhostDustingRecipe extends Logging {
  val SERIALIZER: RegistryObject[GhostDustingRecipeSerializer] = RECIPE_SERIALIZERS.register("ghost_dusting", () => new GhostDustingRecipeSerializer)

  def getSerializer = SERIALIZER.get

  @SubscribeEvent def onConstructMod(event: FMLConstructModEvent) = {}

  @OnlyIn(Dist.CLIENT)
  @SubscribeEvent def onClientSetup(event: FMLClientSetupEvent) = addForgeListeners(onItemTooltip)


  private val gson = new Gson

  case class GhostDustingIngredient (item : String, transparency: Float, limit: Float, clamp: Boolean)

  class GhostDustingRecipeSerializer extends ForgeRegistryEntry[IRecipeSerializer[_]] with IRecipeSerializer[GhostDustingRecipe] {
    override def read(recipeId: ResourceLocation, json: JsonObject): GhostDustingRecipe = {
      logger.info(s"Attempting to read recipe from JSON... ")
      val itemsJson = json.getAsJsonArray("items")
      val ingredients = for(itemNode <- itemsJson.iterator().asScala) yield gson.fromJson(itemNode, classOf[GhostDustingIngredient])

      logger.info(s"Ingredients: $ingredients")
      new GhostDustingRecipe(ingredients.toSeq)
    }
    override def read(recipeId: ResourceLocation, buffer: PacketBuffer): GhostDustingRecipe = {
            logger.info(s"Received packet about GhostDustingRecipe... doing nothing")
//      val stream = buffer.readString
//      logger.info(s"Read raw: $stream from packet")
//      val json = new JsonParser().parse(stream).getAsJsonArray
//      val ingredients = for(itemNode <- json.iterator().asScala) yield gson.fromJson(itemNode, classOf[GhostDustingIngredient])
//      logger.info(s"Read decoded: $json from packet")
      new GhostDustingRecipe(Seq.empty)
    }
    override def write(buffer: PacketBuffer, recipe: GhostDustingRecipe): Unit = {
      logger.info(s"Writing packet about GhostDustingRecipe..by doing nothing")
//      val ingredientsAsJava = recipe.ingredients.toArray
//      logger.info(s"Writing raw: $ingredientsAsJava to packet")
//      val json = gson.toJson(ingredientsAsJava)
//      logger.info(s"Writing string $json to packet")
//      buffer.writeString(json)
    }
  }

  @OnlyIn(Dist.CLIENT)
  def onItemTooltip(event: ItemTooltipEvent): Unit = {
    val stack = event.getItemStack
    if(stack.hasTransparency) event.getToolTip.add(new TranslationTextComponent(s"tooltip.${Anima.MODID}.transparency", (stack.getTransparency * 100).toString).mergeStyle(TextFormatting.GRAY, TextFormatting.ITALIC))
  }
}

@Mod.EventBusSubscriber(modid = Anima.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
case class GhostDustingRecipe(ingredients: Seq[GhostDustingIngredient]) extends ICraftingRecipe with Logging {
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
