package net.machinemuse.anima
package claypot

import net.minecraft.data.ShapedRecipeBuilder
import net.minecraft.data.loot.BlockLootTables.dropping
import net.minecraft.item.Items
import net.minecraftforge.client.model.generators.ConfiguredModel
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent

import claypot.ClayPotBlock._
import util.DatagenHelpers.{FancyShapedRecipeBuilder, SimplerBlockLootTable, existingModModelFile, mkAllVariantBlockStates, mkLanguageProvider, mkRecipeProvider, mkSimpleBlockItemModel, provideBlockLootTable}
import util.Logging

/**
 * Created by MachineMuse on 2/17/2021.
 */
@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
object ClayPotDatagen extends Logging {

  @SubscribeEvent def onGatherData(implicit event: GatherDataEvent): Unit = {
    mkRecipeProvider{ consumer =>
      ShapedRecipeBuilder.shapedRecipe(ClayPotBlock.ITEM.get)
        .patternLine(" / ")
        .patternLine("# #")
        .patternLine("###")
        .addKeyAsCriterion('/', Items.STICK)
        .addKeyAsCriterion('#', Items.CLAY_BALL)
        .setGroup("clay_pot")
        .buildProperly(consumer, "clay_pot")
    }
    mkLanguageProvider("en_us"){ lang =>
      // adds as a block i guess because it's an ItemBlock
      lang.addItem(ClayPotBlock.ITEM, "Clay Pot")
    }
    mkLanguageProvider("fr_fr"){ lang =>
      lang.addItem(ClayPotBlock.ITEM, "Pot en Argile")
    }
    mkAllVariantBlockStates(BLOCK.get){ state =>
      if(state.get(ClayPotBlock.OPEN)) {
        ConfiguredModel.builder()
          .modelFile(existingModModelFile("block/clay_pot_open"))
          .rotationY(state.get(ClayPotBlock.FACING).getHorizontalIndex * 90)
          .build()
      } else {
        ConfiguredModel.builder()
          .modelFile(existingModModelFile("block/clay_pot"))
          .rotationY(state.get(ClayPotBlock.FACING).getHorizontalIndex * 90)
          .build()
      }
    }
    mkSimpleBlockItemModel(ClayPotBlock.BLOCK.get, existingModModelFile("block/clay_pot"))

    provideBlockLootTable {
      new SimplerBlockLootTable {
        add(ClayPotBlock.BLOCK.get, dropping(ClayPotBlock.ITEM.get))
      }
    }

  }
}
