package net.machinemuse.anima.datagen

import net.machinemuse.anima.Anima
import net.machinemuse.anima.registration.AnimaRegistry
import net.machinemuse.anima.util.VanillaClassEnrichers._
import net.minecraft.block.Blocks
import net.minecraft.data.{ShapedRecipeBuilder, ShapelessRecipeBuilder}
import net.minecraft.item.Items
import net.minecraft.tags.ItemTags
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent
import org.apache.logging.log4j.LogManager

/**
 * Created by MachineMuse on 1/28/2021.
 */
@Mod.EventBusSubscriber(modid = Anima.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
object AnimaDatagen {
  private val LOGGER = LogManager.getLogger


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
    }

  }

}
