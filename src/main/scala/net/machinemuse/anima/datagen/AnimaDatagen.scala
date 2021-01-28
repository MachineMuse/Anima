package net.machinemuse.anima.datagen

import net.machinemuse.anima.Anima
import net.machinemuse.anima.registration.AnimaRegistry
import net.minecraft.block.Blocks
import net.minecraft.data.RecipeProvider.hasItem
import net.minecraft.data.{DataGenerator, IFinishedRecipe, RecipeProvider, ShapedRecipeBuilder}
import net.minecraft.item.Items
import net.minecraft.tags.ItemTags
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.crafting.conditions.IConditionBuilder
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent
import org.apache.logging.log4j.LogManager

import java.util.function.Consumer

/**
 * Created by MachineMuse on 1/28/2021.
 */
@Mod.EventBusSubscriber(modid = Anima.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
object AnimaDatagen {
  private val LOGGER = LogManager.getLogger


  //mod bus event
  @SubscribeEvent
  def gatherData(event: GatherDataEvent): Unit = {
    LOGGER.info("received GatherDataEvent from forge")
    val gen: DataGenerator = event.getGenerator
    if(event.includeServer()) {
      LOGGER.info("server side so adding provider")
      gen.addProvider(new KindlingRecipeProvider(gen))
    }
  }

  class KindlingRecipeProvider(generator: DataGenerator) extends RecipeProvider(generator) with IConditionBuilder { // with IConditionBuilder if applicable
    override def registerRecipes(consumer: Consumer[IFinishedRecipe]): Unit = {

      LOGGER.info("registering Campfire recipe")
      ShapedRecipeBuilder.shapedRecipe(Blocks.CAMPFIRE.asItem())
        .patternLine(" / ")
        .patternLine("/K/")
        .patternLine("LLL")
        .key('/', Items.STICK)
        .key('K', AnimaRegistry.KINDLING_ITEM.get())
        .key('L', ItemTags.LOGS_THAT_BURN)
        .addCriterion("has_material", hasItem(Items.STICK))
        .addCriterion("has_material", hasItem(AnimaRegistry.KINDLING_ITEM.get()))
        .addCriterion("has_material", hasItem(ItemTags.LOGS_THAT_BURN))
        .build(consumer, new ResourceLocation(Anima.MODID, "campfire_from_kindling"))
      LOGGER.info("registered Campfire recipe")
      // do NOT call super() as that will generate all the vanilla recipes!
    }
  }
}
