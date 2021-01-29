package net.machinemuse.anima.item.basket

import net.machinemuse.anima.Anima
import net.machinemuse.anima.registration.AnimaRegistry
import net.machinemuse.anima.util.VanillaClassEnrichers.{FancyShapedRecipeBuilder, mkRecipeProvider}
import net.minecraft.data.ShapedRecipeBuilder
import net.minecraft.item.Items
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent
import org.apache.logging.log4j.LogManager

/**
 * Created by MachineMuse on 1/28/2021.
 */
@Mod.EventBusSubscriber(modid = Anima.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
object BasketDatagen {
  private val LOGGER = LogManager.getLogger

  @SubscribeEvent
  def gatherData(event: GatherDataEvent): Unit = mkRecipeProvider(event) {
    consumer =>
      ShapedRecipeBuilder
        .shapedRecipe(AnimaRegistry.BASKET_ITEM.get())
        .patternLine(" / ")
        .patternLine("# #")
        .patternLine("###")
        .addKeyAsCriterion('/', Items.STICK)
        .addKeyAsCriterion('#', Items.SUGAR_CANE)
        .setGroup("basket")
        .buildProperly(consumer, "basket_from_sugar_cane")

      ShapedRecipeBuilder
        .shapedRecipe(AnimaRegistry.BASKET_ITEM.get())
        .patternLine(" / ")
        .patternLine("# #")
        .patternLine("###")
        .addKeyAsCriterion('/', Items.STICK)
        .addKeyAsCriterion('#', Items.BAMBOO)
        .setGroup("basket")
        .buildProperly(consumer, "basket_from_bamboo")

    }
}
