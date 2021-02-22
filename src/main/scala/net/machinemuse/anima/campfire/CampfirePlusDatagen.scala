package net.machinemuse.anima
package campfire

import net.minecraft.data.loot.BlockLootTables
import net.minecraft.data.loot.BlockLootTables.droppingWithSilkTouch
import net.minecraft.item.Items
import net.minecraft.loot.functions.SetCount
import net.minecraft.loot.{ConstantRange, ItemLootEntry}
import net.minecraft.tags.BlockTags
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent

import util.DatagenHelpers.{SimplerBlockLootTable, mkBlockTagsProvider, mkLanguageProvider, provideBlockLootTable}
import util.Logging

/**
 * Created by MachineMuse on 2/17/2021.
 */
@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
object CampfirePlusDatagen extends Logging {

  @SubscribeEvent def gatherData(implicit event: GatherDataEvent): Unit = {
    // Add CampfirePlus to the list of valid Campfires so e.g. flint & steel can light them if doused
    mkBlockTagsProvider(BlockTags.CAMPFIRES){ builder =>
      builder.add(CampfirePlus.getBlock)
    }
    mkLanguageProvider("en_us"){ lang =>
      lang.addBlock(CampfirePlus.BLOCK, "Enhanced Campfire")
    }
    mkLanguageProvider("fr_fr"){ lang =>
      lang.addBlock(CampfirePlus.BLOCK, "Feu de Camp Amélioré")
    }

    provideBlockLootTable {
      new SimplerBlockLootTable {
        /*_*/
        add(CampfirePlus.BLOCK.get, block =>
          droppingWithSilkTouch(block,
            BlockLootTables.withSurvivesExplosion(block,
              ItemLootEntry.builder(Items.CHARCOAL).acceptFunction(SetCount.builder(ConstantRange.of(2)))
            )
          )
        )
        /*_*/
      }
    }
  }
}
