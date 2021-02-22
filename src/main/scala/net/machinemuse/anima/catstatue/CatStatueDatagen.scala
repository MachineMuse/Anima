package net.machinemuse.anima
package catstatue

import net.minecraft.data.ShapedRecipeBuilder
import net.minecraft.data.loot.BlockLootTables.dropping
import net.minecraft.item.Items
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent

import catstatue.CatStatue._
import util.DatagenHelpers.{FancyShapedRecipeBuilder, PartBuilderWorkaround, SimplerBlockLootTable, existingModModelFile, mkLanguageProvider, mkMultipartBlockStates, mkRecipeProvider, mkSimpleBlockItemModel, provideBlockLootTable}
import util.Logging

/**
 * Created by MachineMuse on 2/17/2021.
 */
@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
object CatStatueDatagen extends Logging {

  @SubscribeEvent def onGatherData(implicit event: GatherDataEvent): Unit = {
    mkRecipeProvider{ consumer =>
      ShapedRecipeBuilder.shapedRecipe(CatStatue.ITEM.get)
        .patternLine("#  ")
        .patternLine(" ##")
        .patternLine("# #")
        .addKeyAsCriterion('#', Items.CLAY_BALL)
        .setGroup("cat_statue")
        .buildProperly(consumer, "cat_statue")
    }
    mkLanguageProvider("en_us") { lang =>
      // adds as a block i guess because it's an ItemBlock
      lang.addItem(CatStatue.ITEM, "Clay Cat Statue")
    }
    mkLanguageProvider("fr_fr") { lang =>
      lang.addItem(CatStatue.ITEM, "Statue de Chat en Argile")
    }
    mkMultipartBlockStates(CatStatue.BLOCK.get){ builder =>
      val catModel = existingModModelFile("block/cat_statue_base")
      val waterModels = List(existingModModelFile("block/cat_statue_water1"),
        existingModModelFile("block/cat_statue_water2"),
        existingModModelFile("block/cat_statue_water3"))
      for(index <- HORIZONTAL_DIRECTIONS.indices) {
        for(i <- 1 to 3) {
          builder.part.modelFile(waterModels(i-1)).rotationY(index * 90).addModel()
            .saferCondition(FACING, HORIZONTAL_DIRECTIONS(index))
            .saferCondition(WATERLEVEL, i)
        }
        builder.part.modelFile(catModel).rotationY(index * 90).addModel()
          .saferCondition(FACING, HORIZONTAL_DIRECTIONS(index))
      }
    }
    mkSimpleBlockItemModel(CatStatue.BLOCK.get, existingModModelFile("block/cat_statue_base"))
    provideBlockLootTable {
      new SimplerBlockLootTable {
        /*_*/
        add(CatStatue.BLOCK.get, dropping(CatStatue.ITEM.get))
        /*_*/
      }
    }
  }
}
