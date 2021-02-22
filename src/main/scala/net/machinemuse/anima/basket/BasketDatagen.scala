package net.machinemuse.anima
package basket

import net.minecraft.data.ShapedRecipeBuilder
import net.minecraft.item.Items
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent
import org.apache.logging.log4j.scala.Logging

import basket.Basket.BASKET_ITEM
import util.DatagenHelpers.{FancyShapedRecipeBuilder, mkLanguageProvider, mkRecipeProvider}

/**
 * Created by MachineMuse on 2/17/2021.
 */
@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
object BasketDatagen extends Logging {
  @SubscribeEvent
  def gatherData(implicit event: GatherDataEvent): Unit = {
    logger.trace("BasketDatagen.gatherData called")
    mkRecipeProvider { consumer =>
      ShapedRecipeBuilder.shapedRecipe(BASKET_ITEM.get)
        .patternLine(" / ")
        .patternLine("# #")
        .patternLine("###")
        .addKeyAsCriterion('/', Items.STICK)
        .addKeyAsCriterion('#', Items.SUGAR_CANE)
        .setGroup("basket")
        .buildProperly(consumer, "basket_from_sugar_cane")

      ShapedRecipeBuilder.shapedRecipe(BASKET_ITEM.get)
        .patternLine(" / ")
        .patternLine("# #")
        .patternLine("###")
        .addKeyAsCriterion('/', Items.STICK)
        .addKeyAsCriterion('#', Items.BAMBOO)
        .setGroup("basket")
        .buildProperly(consumer, "basket_from_bamboo")
    }

    mkLanguageProvider("en_us"){ lang =>
      lang.addItem(BASKET_ITEM.supplier, "Basket")
      lang.addScreen("basket", "Basket")
    }
    mkLanguageProvider("fr_fr"){ lang =>
      lang.addItem(BASKET_ITEM.supplier, "Panier")
      lang.addScreen("basket", "Panier")
    }
  }
}
