package net.machinemuse.anima
package campfire

import com.google.gson.JsonObject
import com.mojang.serialization.Codec
import net.minecraft.data.IFinishedRecipe
import net.minecraft.entity.EntityType
import net.minecraft.inventory.CraftingInventory
import net.minecraft.item._
import net.minecraft.item.crafting.{ICraftingRecipe, IRecipeSerializer}
import net.minecraft.util.ResourceLocation
import net.minecraft.world.World
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.{FMLConstructModEvent, GatherDataEvent}
import org.apache.logging.log4j.scala.Logging

import bowl.BowlWithContents
import campfire.CampfireDustRecipe.CampfireDustIngredient
import entity.EntityLightSpirit
import registration.RegistryHelpers
import util.Colour
import util.DatagenHelpers._
import util.GenCodecsByName._
import util.VanillaCodecs._


/**
 * Created by MachineMuse on 2/6/2021.
 */
object CampfireDustRecipe extends Logging {
  @SubscribeEvent def onConstructMod(event: FMLConstructModEvent) = {}

  private val RecipeCodec = /*_*/ implicitly[Codec[CampfireDustRecipe]] /*_*/
  private val SERIALIZER = RegistryHelpers.regRecipeSerializer("campfire_dust", RecipeCodec, new CampfireDustRecipe(List.empty, List.empty))

  sealed trait CampfireDustEffect extends CodecByName
  case class InnerColour(innerColour: Int) extends CampfireDustEffect with CodecByName
  case class OuterColour(outerColour: Int) extends CampfireDustEffect with CodecByName
  case class Attracts(entityTypes: List[EntityType[_]]) extends CampfireDustEffect with CodecByName
  /*_*/
  implicit val CampfireDustEffectCodec = implicitly[Codec[CampfireDustEffect]]
  /*_*/

//  /*_*/ private val EffectCodec = implicitly[Codec[CampfireDustEffect]] /*_*/

  case class CampfireDustIngredient(item: Item,
                                    outercolour: Option[Int] = None,
                                    innercolour: Option[Int] = None,
                                    attracts: Option[EntityType[_]] = None) extends CodecByName

  @SubscribeEvent
  def gatherData(implicit event: GatherDataEvent): Unit = {
    mkRecipeProvider { consumer =>
      val colours = DyeColor.values().map(color => CampfireDustIngredient(DyeItem.getItem(color), outercolour = color.getColorValue.some, innercolour = color.getTextColor.some))
      val defaultRecipe = CampfireDustRecipe(List(Items.GUNPOWDER, BowlWithContents.BOWL_OF_SALT.get), List(
        CampfireDustIngredient(Items.CHARCOAL, attracts = EntityLightSpirit.getType.some)
      ) ++ colours)
      consumer.accept(defaultRecipe)
    }
  }

}

@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
case class CampfireDustRecipe(bases: List[Item], ingredients: List[CampfireDustIngredient]) extends ICraftingRecipe with CodecByName with IFinishedRecipe with Logging {

  import campfire.CampfireDustRecipe._
  override def matches(inv: CraftingInventory, worldIn: World): Boolean = {
    val stacks = (for (i <- 0 until inv.getSizeInventory) yield inv.getStackInSlot(i)).toList.filter(!_.isEmpty)
    var missingBases = bases
    var fail = false
    for (stack <- stacks) {
      val matchingIngredients = ingredients.filter(_.item == stack.getItem)
      val matchingBases = bases.filter(_ == stack.getItem)
      missingBases = missingBases.diff(matchingBases)
      if (matchingIngredients.isEmpty && matchingBases.isEmpty) fail = true

    }
    missingBases.isEmpty && !fail
  }

  override def getCraftingResult(inv: CraftingInventory): ItemStack = {
    val output = new ItemStack(DustForCampfire.CAMPFIRE_DUST_ITEM.get)
    val stacks = (for (i <- 0 until inv.getSizeInventory) yield inv.getStackInSlot(i)).toList.filter(!_.isEmpty)
    var innerColour = 0
    var innerColoursFound = 0
    var outerColour = 0
    var outerColoursFound = 0
    var missingBases = bases
    var fail = false
    var attracts = List.empty[EntityType[_]]
    for (stack <- stacks) {
      val matchingIngredients = ingredients.filter(_.item == stack.getItem)
      val matchingBases = bases.filter(_ == stack.getItem)
      missingBases = missingBases.diff(matchingBases)
      if (matchingIngredients.isEmpty && matchingBases.isEmpty) fail = true
      matchingIngredients.flatMap(_.outercolour).foreach { foundcolour =>
        outerColour = Colour.mixColoursByWeight(outerColour, foundcolour, outerColoursFound.toFloat, 1.0f)
        outerColoursFound += 1
      }
      matchingIngredients.flatMap(_.innercolour).foreach { foundcolour =>
        innerColour = Colour.mixColoursByWeight(innerColour, foundcolour, innerColoursFound.toFloat, 1.0f)
        innerColoursFound += 1
      }
      matchingIngredients.flatMap(_.attracts).foreach { foundAttracts =>
        attracts = attracts :+ foundAttracts
      }
    }
    if (innerColoursFound > 0) {
      output.getOrCreateTag.putInt("colour1", outerColour)
    }
    if (outerColoursFound > 0) {
      output.getOrCreateTag().putInt("colour2", innerColour)
    }
    if(innerColoursFound > 0 || outerColoursFound> 0) {
      output.getOrCreateTag().putInt("colour0", Colour.mixColoursByWeight(innerColour, outerColour, innerColoursFound.toFloat, outerColoursFound.toFloat))
    }
    if(attracts.nonEmpty) {
      /*_*/
      import util.VanillaCodecs._
      val nbt = implicitly[Codec[List[EntityType[_]]]].writeINBT(attracts)
      /*_*/
      output.getOrCreateTag().put("attracts", nbt)
    }

    output
  }

  override def canFit(width: Int, height: Int): Boolean = width * height >= 3

  override def getRecipeOutput: ItemStack = ItemStack.EMPTY

  override def getID: ResourceLocation = getId
  override def getId: ResourceLocation = new ResourceLocation(implicitly[MODID], "campfire_dust")

  override def getSerializer: IRecipeSerializer[_] = SERIALIZER.get
  override def serialize(jsonOut: JsonObject): Unit =
    RecipeCodec.writeIntoMutableJson(this, jsonOut).andDo(_ => if (getGroup.nonEmpty) jsonOut.addProperty("group", getGroup))

  // No advancements associated with 'unlocking' this recipe
  override def getAdvancementJson: JsonObject = null
  override def getAdvancementID: ResourceLocation = null
}