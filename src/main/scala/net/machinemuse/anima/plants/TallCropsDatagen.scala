package net.machinemuse.anima
package plants

import net.minecraft.advancements.criterion.StatePropertiesPredicate
import net.minecraft.block.CropsBlock
import net.minecraft.data.ShapelessRecipeBuilder
import net.minecraft.data.loot.BlockLootTables.droppingAndBonusWhen
import net.minecraft.item.Items
import net.minecraft.loot.conditions.BlockStateProperty
import net.minecraft.tags.BlockTags
import net.minecraftforge.client.model.generators.ConfiguredModel
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent
import org.apache.logging.log4j.scala.Logging

import plants.TallCropsBlock._
import util.DatagenHelpers._

/**
 * Created by MachineMuse on 2/17/2021.
 */
@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
object TallCropsDatagen extends Logging {
  @SubscribeEvent
  def gatherData(implicit event: GatherDataEvent): Unit = {
    mkBlockTagsProvider(BlockTags.CROPS) { builder =>
      builder.add(TallCropsBlock.WOAD_LEAVES_BLOCK.get)
    }
    mkRecipeProvider{ consumer =>
      ShapelessRecipeBuilder.shapelessRecipe(Items.BLUE_DYE)
        .addIngredientAsCriterion("woad_leaves", WOAD_LEAVES_ITEM.get)
        .setGroup("dyes")
        .buildProperly(consumer, "blue_dye_from_woad_leaves")
    }
    mkLanguageProvider("en_us") { lang =>
      lang.addBlock(WOAD_LEAVES_BLOCK, "Woad Leaves")
      lang.addBlock(WOAD_FLOWERS_BLOCK, "Woad Flowers")
      lang.addItem(WOAD_SEEDS, "Woad Seeds")
      lang.addItem(WOAD_LEAVES_ITEM, "Woad Leaves")
    }
    mkLanguageProvider("fr_fr") { lang =>
      lang.addBlock(WOAD_LEAVES_BLOCK, "Feuilles de Guêpe")
      lang.addBlock(WOAD_FLOWERS_BLOCK, "Fleurs de Guêpe")
      lang.addItem(WOAD_SEEDS, "Graines de Guêpe")
      lang.addItem(WOAD_LEAVES_ITEM, "Feuilles de Guêpe")
    }
    provideBlockLootTable {
      new SimplerBlockLootTable {
        /*_*/
        val lootConditionBuilder = BlockStateProperty.builder(WOAD_LEAVES_BLOCK.get)
          .fromProperties(StatePropertiesPredicate.Builder.newBuilder.withIntProp(CropsBlock.AGE, 7))
        add(WOAD_LEAVES_BLOCK.get, droppingAndBonusWhen(WOAD_LEAVES_BLOCK.get, WOAD_SEEDS.get, WOAD_LEAVES_ITEM.get, lootConditionBuilder))
        /*_*/
      }
    }
    mkSimpleItemModel(WOAD_SEEDS.get)
    mkSimpleItemModel(WOAD_LEAVES_ITEM.get, "block/woadleavesfull")
    mkBlockModel { provider =>
      for(name <- List("woadsprout", "woadleavestrimmed", "woadleavesfull", "woadflowers")) {
        provider.crop(name, modLoc(s"block/$name"))
      }
    }
    mkAllVariantBlockStates(WOAD_LEAVES_BLOCK.get) {state =>
      state.get(TallCropsBlock.AGE).intValue() match {
        case 0 | 1 =>
          ConfiguredModel.builder()
            .modelFile(existingModModelFile("block/woadsprout"))
            .build()
        case 2 | 3 | 4 | 5 | 6 => ConfiguredModel.builder()
          .modelFile(existingModModelFile("block/woadleavestrimmed"))
          .build()
        case 7 => ConfiguredModel.builder()
          .modelFile(existingModModelFile("block/woadleavesfull"))
          .build()
        case _ => throw new Exception("Age went beyond 0..7 when generating woad blockstates? Unexpected")
      }
    }
    mkSimpleBlockState(WOAD_FLOWERS_BLOCK.get, existingModModelFile("block/woadflowers"))

  }
}
