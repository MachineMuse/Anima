package net.machinemuse.anima
package plants

import net.minecraft.advancements.criterion.StatePropertiesPredicate
import net.minecraft.block.CropsBlock
import net.minecraft.data.loot.BlockLootTables.droppingAndBonusWhen
import net.minecraft.loot.conditions.BlockStateProperty
import net.minecraft.tags.BlockTags
import net.minecraftforge.client.model.generators.ConfiguredModel
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent

import util.DatagenHelpers._
import util.Logging

/**
 * Created by MachineMuse on 2/19/2021.
 */
@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
object RadishDatagen extends Logging {

  @SubscribeEvent
  def gatherData(implicit event: GatherDataEvent): Unit = {

    mkBlockTagsProvider(BlockTags.CROPS) { builder =>
      builder.add(Radish.BLOCK.get)
    }
    mkLanguageProvider("en_us") { lang =>
      lang.addBlock(Radish.BLOCK, "Radish")
      lang.addItem(Radish.ITEM, "Radish")
      lang.addItem(Radish.SEEDS_ITEM, "Radish Seeds")
    }
    mkLanguageProvider("fr_fr") { lang =>
      lang.addBlock(Radish.BLOCK, "Radis")
      lang.addItem(Radish.ITEM, "Radis")
      lang.addItem(Radish.SEEDS_ITEM, "Graines de Radis")
    }
    provideBlockLootTable {
      new SimplerBlockLootTable {
        /*_*/
        val lootConditionBuilder = BlockStateProperty.builder(Radish.BLOCK.get)
          .fromProperties(StatePropertiesPredicate.Builder.newBuilder.withIntProp(CropsBlock.AGE, 7))
        add(Radish.BLOCK.get, droppingAndBonusWhen(Radish.BLOCK.get, Radish.ITEM.get, Radish.SEEDS_ITEM.get, lootConditionBuilder))
        /*_*/
      }
    }
    mkSimpleItemModel(Radish.SEEDS_ITEM.get)
    mkSimpleItemModel(Radish.ITEM.get)
    mkBlockModel { provider =>
      for(name <- List("radish_sprouts", "radish_growing", "radish_almost_grown", "radish_grown")) {
        provider.crop(name, modLoc(s"block/$name"))
      }
    }
    mkAllVariantBlockStates(Radish.BLOCK.get) { state =>
      state.get(TallCropsBlock.AGE).intValue() match {
        case 0 | 1 =>
          ConfiguredModel.builder()
            .modelFile(existingModModelFile("block/radish_sprouts"))
            .build()
        case 2 | 3 | 4 => ConfiguredModel.builder()
          .modelFile(existingModModelFile("block/radish_growing"))
          .build()
        case 5 | 6 => ConfiguredModel.builder()
          .modelFile(existingModModelFile("block/radish_almost_grown"))
          .build()
        case 7 => ConfiguredModel.builder()
          .modelFile(existingModModelFile("block/radish_grown"))
          .build()
        case _ => throw new Exception("Age went beyond 0..7 when generating woad blockstates? Unexpected")
      }
    }

  }
}
