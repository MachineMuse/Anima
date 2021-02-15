package net.machinemuse.anima
package registration

import net.minecraft.block.Blocks
import net.minecraft.data.ShapedRecipeBuilder
import net.minecraft.item.{Item, Items}
import net.minecraft.tags.ItemTags
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.{FMLConstructModEvent, GatherDataEvent}

import util.DatagenHelpers._

/**
 * Created by MachineMuse on 1/21/2021.
 */
@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
object SimpleItems {
  import RegistryHelpers._

  @SubscribeEvent def onConstructMod(e: FMLConstructModEvent) = {}

  val SPIRITFIRE_ITEM = regSimpleItem("spiritfire", new Item.Properties())
  val AnimaCreativeGroup = regCreativeTab(() => SPIRITFIRE_ITEM)

  val KINDLING_ITEM = regSimpleItem("kindling")


  val CAMPFIRE_ASH = regSimpleItem("campfire_ash")

  @SubscribeEvent
  def gatherData(implicit event: GatherDataEvent): Unit = {
    mkRecipeProvider { consumer =>

      ShapedRecipeBuilder
        .shapedRecipe(Blocks.CAMPFIRE.asItem())
        .patternLine(" / ")
        .patternLine("/K/")
        .patternLine("LLL")
        .addKeyAsCriterion('/', Items.STICK)
        .addKeyAsCriterion('K', SimpleItems.KINDLING_ITEM.get)
        .addKeyAsCriterion('L', ItemTags.LOGS_THAT_BURN)
        .buildProperly(consumer, "campfire_from_kindling")

      ShapedRecipeBuilder
        .shapedRecipe(SimpleItems.KINDLING_ITEM.get)
        .patternLine("///")
        .patternLine("/P/")
        .patternLine("///")
        .addKeyAsCriterion('/', Items.STICK)
        .addKeyAsCriterion('P', ItemTags.PLANKS)
        .buildProperly(consumer, "kindling")

      CampfireRecipeBuilder.campfireRecipe(ItemTags.LOGS_THAT_BURN, Items.CHARCOAL, 0.35F)
        .buildProperly(consumer, "charcoal_on_campfire")

      CampfireRecipeBuilder.campfireRecipe(ItemTags.COALS, CAMPFIRE_ASH.get, 0.35F)
        .buildProperly(consumer, "ash_on_campfire")
    }
    mkLanguageProvider("en_us") { lang =>
      lang.addItem(KINDLING_ITEM.supplier, "Kindling")
      lang.addItem(CAMPFIRE_ASH.supplier, "Campfire Ash")

      lang.addCreativeGroup("Anima")
    }
    mkLanguageProvider("fr_fr") { lang =>
      lang.addItem(KINDLING_ITEM.supplier, "Petit Bois d'Allumage")
      lang.addItem(CAMPFIRE_ASH.supplier, "Cendres de Feu de Camp")

      lang.addCreativeGroup("Anima")
    }
  }
}
