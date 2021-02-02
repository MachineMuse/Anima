package net.machinemuse.anima
package util

import net.minecraft.advancements.criterion._
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType
import net.minecraft.data._
import net.minecraft.entity.player.{PlayerEntity, PlayerInventory}
import net.minecraft.inventory.container.{Container, INamedContainerProvider}
import net.minecraft.item.crafting.{IRecipeSerializer, Ingredient}
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.tags.ITag
import net.minecraft.util.text.{ITextComponent, TranslationTextComponent}
import net.minecraft.util.{Unit => _, _}
import net.minecraftforge.common.crafting.conditions.IConditionBuilder
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent
import org.apache.logging.log4j.scala.Logging

import java.util.function.Consumer

/**
 * Created by MachineMuse on 1/27/2021.
 */
object VanillaClassEnrichers extends Logging {
  implicit class RichTransformType(transformType: TransformType) {
    def isLeftHand = transformType == TransformType.FIRST_PERSON_LEFT_HAND || transformType == TransformType.FIRST_PERSON_LEFT_HAND
  }

  implicit class RichPlayerEntity(player: PlayerEntity) {
    def itemInHand: Hand => ItemStack = {
      case Hand.MAIN_HAND => player.inventory.mainInventory.get(player.inventory.currentItem)
      case Hand.OFF_HAND => player.inventory.offHandInventory.get(0)
    }
  }

  implicit class RichItemStack(stack: ItemStack) {
    private val tagName = "transparency"
    def hasTransparency: Boolean = {
      stack.hasTag && stack.getTag.contains(tagName)
    }
    def getTransparency: Float = {
      if(stack.hasTransparency) {
        stack.getTag.getFloat(tagName)
      } else {
        0.0F
      }
    }
    def setTransparency(f: Float): Unit = {
      stack.getOrCreateTag().putFloat(tagName, f)
    }

    def removeTransparency(): Unit = {
      stack.getOrCreateTag().remove(tagName)
    }
  }


  def mkContainerProvider[T <: Container](name: String, menuctor: (Int, PlayerInventory, PlayerEntity) => T): INamedContainerProvider = {
    new INamedContainerProvider() {
      override def getDisplayName: ITextComponent = new TranslationTextComponent(s"screen.${Anima.MODID}.$name")

      override def createMenu(windowId: Int, playerInventory: PlayerInventory, playerEntity: PlayerEntity): T = menuctor(windowId, playerInventory, playerEntity)
    }
  }

  def mkRecipeProvider(event: GatherDataEvent)(reg: Consumer[IFinishedRecipe] => ()): Unit = {
    if(event.includeServer()) {
      val prov = new RecipeProvider (event.getGenerator) with IConditionBuilder {
        override def registerRecipes (consumer: Consumer[IFinishedRecipe] ): Unit = {
          // do NOT call super() as that will generate all the vanilla recipes!
          reg (consumer)
        }
      }
      event.getGenerator.addProvider (prov)
    }
  }

  implicit class FancyShapedRecipeBuilder(builder: ShapedRecipeBuilder) {
    def addKeyAsCriterion(c: Char, i: Item): ShapedRecipeBuilder = {
      builder.key(c, i)
      builder.addCriterion("has_" + c, hasItem(i))
    }
    def addKeyAsCriterion(c: Char, i: ITag[Item]): ShapedRecipeBuilder = {
      builder.key(c, i)
      builder.addCriterion("has_" + c, hasItem(i))
    }
    def buildProperly(consumer: Consumer[IFinishedRecipe], filename: String) = {
      builder.build((a: IFinishedRecipe) => {
        logger.info("Built recipe: " + a.getID)
        consumer.accept(a)
      }, new ResourceLocation(Anima.MODID, filename))
    }
  }

  implicit class FancyShapelessRecipeBuilder(builder: ShapelessRecipeBuilder) {
    def addIngredientAsCriterion(name: String, ingredient: Item) = {
      builder.addIngredient(ingredient)
      builder.addCriterion("has_" + name, hasItem(ingredient))
    }
    def addIngredientAsCriterion(name: String, ingredient: ITag[Item]) = {
      builder.addIngredient(ingredient)
      builder.addCriterion("has_" + name, hasItem(ingredient))
    }
    def buildProperly(consumer: Consumer[IFinishedRecipe], filename: String) = {
      builder.build((a: IFinishedRecipe) => {
        logger.info("Built recipe: " + a.getID)
        consumer.accept(a)
      }, new ResourceLocation(Anima.MODID, filename))
    }
  }
  object CampfireRecipeBuilder {
    def campfireRecipe(ingredient: ITag[Item], output: Item, exp: Float) = {
      CookingRecipeBuilder.cookingRecipe(Ingredient.fromTag(ingredient), output, exp, 600, IRecipeSerializer.CAMPFIRE_COOKING).addCriterion("has_ingredient", hasItem(ingredient))
    }
    def campfireRecipe(ingredient: Item, output: Item, exp: Float) = {
      CookingRecipeBuilder.cookingRecipe(Ingredient.fromItems(ingredient), output, exp, 600, IRecipeSerializer.CAMPFIRE_COOKING).addCriterion("has_ingredient", hasItem(ingredient))
    }
  }
  implicit class CampfireRecipeBuilder(builder: CookingRecipeBuilder) {
    def buildProperly(consumer: Consumer[IFinishedRecipe], filename: String) = {
      builder.build((a: IFinishedRecipe) => {
        logger.info("Built recipe: " + a.getID)
        consumer.accept(a)
      }, new ResourceLocation(Anima.MODID, filename))
    }
  }

  protected def hasItem(item: IItemProvider): InventoryChangeTrigger.Instance = hasItem(ItemPredicate.Builder.create.item(item).build)

  protected def hasItem(tag: ITag[Item]): InventoryChangeTrigger.Instance = hasItem(ItemPredicate.Builder.create.tag(tag).build)

  protected def hasItem(predicate: ItemPredicate*) = new InventoryChangeTrigger.Instance(EntityPredicate.AndPredicate.ANY_AND, MinMaxBounds.IntBound.UNBOUNDED, MinMaxBounds.IntBound.UNBOUNDED, MinMaxBounds.IntBound.UNBOUNDED, predicate.toArray)
}
