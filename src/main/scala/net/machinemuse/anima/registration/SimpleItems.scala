package net.machinemuse.anima
package registration

import util.VanillaClassEnrichers.{CampfireRecipeBuilder, FancyShapedRecipeBuilder, FancyShapelessRecipeBuilder, mkRecipeProvider}

import net.minecraft.block.Blocks
import net.minecraft.data.{ShapedRecipeBuilder, ShapelessRecipeBuilder}
import net.minecraft.item.Items
import net.minecraft.tags.ItemTags
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.{FMLConstructModEvent, GatherDataEvent}

/**
 * Created by MachineMuse on 1/21/2021.
 */
@Mod.EventBusSubscriber(modid = Anima.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
object SimpleItems {
  import RegistryHelpers._

  @SubscribeEvent def onConstructMod(e: FMLConstructModEvent) = {}

  val SPIRITFIRE_ITEM = regSimpleItem("spiritfire", ItemProperties(creativeGroup = Some(null)).some)
  val AnimaCreativeGroup = regCreativeTab(() => SPIRITFIRE_ITEM)

  val KINDLING_ITEM = regSimpleItem("kindling")

  val BIRDBONES_ITEM = regSimpleItem("birdbones")

  val ANIMALBONES_ITEM = regSimpleItem("animalbones")

  val GHOSTDUST_ITEM = regSimpleItem("ghost_dust")

  val GHOSTDUST_REMOVER_ITEM = regSimpleItem("ghost_dust_remover")


  @SubscribeEvent
  def gatherData(event: GatherDataEvent): Unit = {
    mkRecipeProvider(event) { consumer =>

      ShapedRecipeBuilder
        .shapedRecipe(Blocks.CAMPFIRE.asItem())
        .patternLine(" / ")
        .patternLine("/K/")
        .patternLine("LLL")
        .addKeyAsCriterion('/', Items.STICK)
        .addKeyAsCriterion('K', SimpleItems.KINDLING_ITEM.get())
        .addKeyAsCriterion('L', ItemTags.LOGS_THAT_BURN)
        .buildProperly(consumer, "campfire_from_kindling")

      ShapedRecipeBuilder
        .shapedRecipe(SimpleItems.KINDLING_ITEM.get())
        .patternLine("///")
        .patternLine("/P/")
        .patternLine("///")
        .addKeyAsCriterion('/', Items.STICK)
        .addKeyAsCriterion('P', ItemTags.PLANKS)
        .buildProperly(consumer, "kindling")

      ShapelessRecipeBuilder
        .shapelessRecipe(Items.BONE_MEAL)
        .addIngredientAsCriterion("birdbones", SimpleItems.BIRDBONES_ITEM.get)
        .setGroup("bonemeal")
        .buildProperly(consumer, "bonemeal_from_birdbones")

      ShapelessRecipeBuilder
        .shapelessRecipe(Items.BONE_MEAL, 2)
        .addIngredientAsCriterion("animalbones", SimpleItems.ANIMALBONES_ITEM.get)
        .setGroup("bonemeal")
        .buildProperly(consumer, "bonemeal_from_animalbones")

      ShapelessRecipeBuilder
        .shapelessRecipe(SimpleItems.GHOSTDUST_ITEM.get, 2)
        .addIngredientAsCriterion("gunpowder", Items.GUNPOWDER)
        .addIngredientAsCriterion("bonemeal", Items.BONE_MEAL)
        .addIngredientAsCriterion("sugar", Items.SUGAR)
        .setGroup("ghost_dust")
        .buildProperly(consumer, "ghost_dust_from_bonemeal")

      ShapelessRecipeBuilder
        .shapelessRecipe(SimpleItems.GHOSTDUST_ITEM.get, 2)
        .addIngredientAsCriterion("gunpowder", Items.GUNPOWDER)
        .addIngredientAsCriterion("white_dye", Items.WHITE_DYE)
        .addIngredientAsCriterion("sugar", Items.SUGAR)
        .setGroup("ghost_dust")
        .buildProperly(consumer, "ghost_dust_from_white_dye")

      ShapelessRecipeBuilder
        .shapelessRecipe(SimpleItems.GHOSTDUST_REMOVER_ITEM.get, 1)
        .addIngredientAsCriterion("gunpowder", Items.GUNPOWDER)
        .addIngredientAsCriterion("ink_sac", Items.INK_SAC)
        .addIngredientAsCriterion("sugar", Items.SUGAR)
        .setGroup("ghost_dust_remover")
        .buildProperly(consumer, "ghost_dust_remover_from_ink_sac")

      ShapelessRecipeBuilder
        .shapelessRecipe(SimpleItems.GHOSTDUST_REMOVER_ITEM.get, 1)
        .addIngredientAsCriterion("gunpowder", Items.GUNPOWDER)
        .addIngredientAsCriterion("black_dye", Items.BLACK_DYE)
        .addIngredientAsCriterion("sugar", Items.SUGAR)
        .setGroup("ghost_dust_remover")
        .buildProperly(consumer, "ghost_dust_remover_from_black_dye")

      CampfireRecipeBuilder.campfireRecipe(ItemTags.LOGS_THAT_BURN, Items.CHARCOAL, 0.35F)
        .buildProperly(consumer, "charcoal_on_campfire")
    }

  }
}