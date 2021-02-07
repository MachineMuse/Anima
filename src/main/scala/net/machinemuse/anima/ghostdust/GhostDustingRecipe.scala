package net.machinemuse.anima
package ghostdust

import ghostdust.GhostDustingRecipe.GhostDustingIngredient
import registration.RegistryHelpers.RECIPE_SERIALIZERS
import util.VanillaClassEnrichers.RichItemStack
import util.VanillaCodecs._

import com.google.gson._
import com.mojang.serialization.{Codec, JsonOps}
import net.minecraft.inventory.CraftingInventory
import net.minecraft.item.crafting.{ICraftingRecipe, IRecipeSerializer}
import net.minecraft.item._
import net.minecraft.network.PacketBuffer
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.MathHelper
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

/**
 * Created by MachineMuse on 2/2/2021.
 */
object GhostDustingRecipe extends Logging {
  private val gson = new Gson
  private val SERIALIZER: RegistryObject[GhostDustingRecipeSerializer] = RECIPE_SERIALIZERS.register("ghost_dusting", () => new GhostDustingRecipeSerializer)
  def getSerializerInstance = SERIALIZER.get

  @SubscribeEvent def onConstructMod(event: FMLConstructModEvent) = {}


  case class GhostDustingIngredientSerial(item : String, transparency: Float, limit: Int, clamp: Boolean)
  case class GhostDustingIngredient(item : Item, transparency: Float, limit: Int, clamp: Boolean)
  /*_*/
  val GDICodec = new CodecMaker[GhostDustingIngredient].genCaseCodec
  val IngredientsCodec: Codec[List[GhostDustingIngredient]] = SLISTCODEC(GDICodec)
  /*_*/
  class GhostDustingRecipeSerializer extends ForgeRegistryEntry[IRecipeSerializer[_]] with IRecipeSerializer[GhostDustingRecipe] {
    override def read(recipeId: ResourceLocation, json: JsonObject): GhostDustingRecipe = {
      val itemsJson = json.getAsJsonArray("items")
      val ingredients = IngredientsCodec.decode(JsonOps.INSTANCE, itemsJson).resultOrPartial(err => logger.error(s"error reading stream: $err")).get().getFirst
      logger.warn(s"Recipe deserialized from json: $ingredients")
      new GhostDustingRecipe(ingredients)
    }
    override def read(recipeId: ResourceLocation, buffer: PacketBuffer): GhostDustingRecipe = {
      val string = buffer.readString
      val itemsJson = new JsonParser().parse(string).getAsJsonArray
      val ingredients = IngredientsCodec.decode(JsonOps.INSTANCE, itemsJson).resultOrPartial(err => logger.error(s"error reading stream: $err")).get().getFirst
      logger.warn(s"Recipe deserialized from packet: $ingredients")
      new GhostDustingRecipe(ingredients)
    }
    override def write(buffer: PacketBuffer, recipe: GhostDustingRecipe): Unit = {
      val json = IngredientsCodec.encodeStart(JsonOps.INSTANCE, recipe.ingredients).result().get()
      logger.warn(s"Recipe serialized: $json")
      buffer.writeString(json.toString)
    }
  }

  @OnlyIn(Dist.CLIENT)
  @SubscribeEvent def onClientSetup(event: FMLClientSetupEvent) = addForgeListeners(onItemTooltip)

  @OnlyIn(Dist.CLIENT)
  def onItemTooltip(event: ItemTooltipEvent): Unit = {
    val stack = event.getItemStack
    if(stack.hasTransparency) event.getToolTip.add(new TranslationTextComponent(s"tooltip.${Anima.MODID}.transparency", (stack.getTransparency * 100).toString).mergeStyle(TextFormatting.GRAY, TextFormatting.ITALIC))
  }
}

@Mod.EventBusSubscriber(modid = Anima.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
case class GhostDustingRecipe(ingredients: List[GhostDustingIngredient]) extends ICraftingRecipe with Logging {
  override def matches(inv: CraftingInventory, worldIn: World): Boolean = {
    var armorFound = false
    var ghostDustFound = false
    var tooMany = false

    val ghostDustItems = ingredients.map { _.item }
    for(slot <- 0 until inv.getSizeInventory) {
      val stackInSlot = inv.getStackInSlot(slot)
      stackInSlot.getItem match {
        case armorItem: ArmorItem => if(!armorFound) armorFound = true else tooMany = true
        case ghostdust if ghostDustItems.contains(ghostdust) => {
          ghostDustFound = true
        }
        case _ =>
      }
    }

    armorFound && ghostDustFound && !tooMany
  }

  override def getCraftingResult(inv: CraftingInventory): ItemStack = {
    var armorFound = ItemStack.EMPTY
    var tooManyArmors = false
    var ghostDustFound = 0

    val ghostDustItems = ingredients.map { _.item }

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
      for(ingredient <- ingredients) {
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

  override def getSerializer: IRecipeSerializer[GhostDustingRecipe] = GhostDustingRecipe.getSerializerInstance
}
