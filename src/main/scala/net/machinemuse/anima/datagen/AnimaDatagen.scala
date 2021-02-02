package net.machinemuse.anima
package datagen

import registration.AnimaRegistry
import util.VanillaClassEnrichers._

import net.minecraft.block.Blocks
import net.minecraft.data.{ShapedRecipeBuilder, ShapelessRecipeBuilder}
import net.minecraft.item.Items
import net.minecraft.tags.ItemTags
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent

/**
 * Created by MachineMuse on 1/28/2021.
 */
@Mod.EventBusSubscriber(modid = Anima.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
object AnimaDatagen {
  //mod bus event
  @SubscribeEvent
  def gatherData(event: GatherDataEvent): Unit = {
    mkRecipeProvider(event) { consumer =>

      ShapedRecipeBuilder
        .shapedRecipe(Blocks.CAMPFIRE.asItem())
        .patternLine(" / ")
        .patternLine("/K/")
        .patternLine("LLL")
        .addKeyAsCriterion('/', Items.STICK)
        .addKeyAsCriterion('K', AnimaRegistry.KINDLING_ITEM.get())
        .addKeyAsCriterion('L', ItemTags.LOGS_THAT_BURN)
        .buildProperly(consumer, "campfire_from_kindling")

      ShapedRecipeBuilder
        .shapedRecipe(AnimaRegistry.KINDLING_ITEM.get())
        .patternLine("///")
        .patternLine("/P/")
        .patternLine("///")
        .addKeyAsCriterion('/', Items.STICK)
        .addKeyAsCriterion('P', ItemTags.PLANKS)
        .buildProperly(consumer, "kindling")

      ShapelessRecipeBuilder
        .shapelessRecipe(Items.BONE_MEAL)
        .addIngredientAsCriterion("birdbones", AnimaRegistry.BIRDBONES_ITEM.get)
        .setGroup("bonemeal")
        .buildProperly(consumer, "bonemeal_from_birdbones")

      ShapelessRecipeBuilder
        .shapelessRecipe(Items.BONE_MEAL, 2)
        .addIngredientAsCriterion("animalbones", AnimaRegistry.ANIMALBONES_ITEM.get)
        .setGroup("bonemeal")
        .buildProperly(consumer, "bonemeal_from_animalbones")

      ShapelessRecipeBuilder
        .shapelessRecipe(AnimaRegistry.GHOSTDUST_ITEM.get, 2)
        .addIngredientAsCriterion("gunpowder", Items.GUNPOWDER)
        .addIngredientAsCriterion("bonemeal", Items.BONE_MEAL)
        .addIngredientAsCriterion("sugar", Items.SUGAR)
        .setGroup("ghost_dust")
        .buildProperly(consumer, "ghost_dust_from_bonemeal")

      ShapelessRecipeBuilder
        .shapelessRecipe(AnimaRegistry.GHOSTDUST_ITEM.get, 2)
        .addIngredientAsCriterion("gunpowder", Items.GUNPOWDER)
        .addIngredientAsCriterion("white_dye", Items.WHITE_DYE)
        .addIngredientAsCriterion("sugar", Items.SUGAR)
        .setGroup("ghost_dust")
        .buildProperly(consumer, "ghost_dust_from_white_dye")

      ShapelessRecipeBuilder
        .shapelessRecipe(AnimaRegistry.GHOSTDUST_REMOVER_ITEM.get, 1)
        .addIngredientAsCriterion("gunpowder", Items.GUNPOWDER)
        .addIngredientAsCriterion("ink_sac", Items.INK_SAC)
        .addIngredientAsCriterion("sugar", Items.SUGAR)
        .setGroup("ghost_dust_remover")
        .buildProperly(consumer, "ghost_dust_remover_from_ink_sac")

      ShapelessRecipeBuilder
        .shapelessRecipe(AnimaRegistry.GHOSTDUST_REMOVER_ITEM.get, 1)
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
