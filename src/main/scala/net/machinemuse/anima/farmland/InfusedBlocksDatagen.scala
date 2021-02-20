package net.machinemuse.anima
package farmland

import net.minecraft.data.loot.BlockLootTables.dropping
import net.minecraft.item.Items
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent
import org.apache.logging.log4j.scala.Logging

import util.DatagenHelpers._

/**
 * Created by MachineMuse on 2/19/2021.
 */
@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
object InfusedBlocksDatagen extends Logging {
  @SubscribeEvent def gatherData(implicit event: GatherDataEvent): Unit = {
    mkLanguageProvider("en_us") { lang =>
      lang.addBlock(InfusedBasicBlocks.DIRT, "Infused Dirt")
    }
    mkLanguageProvider("fr_fr") { lang =>
      lang.addBlock(InfusedBasicBlocks.DIRT, "Terre Infusée")
    }
    provideBlockLootTable {
      new SimplerBlockLootTable {
        add(InfusedBasicBlocks.DIRT.get, dropping(Items.DIRT))
      }
    }
    mkSimpleBlockState(InfusedBasicBlocks.DIRT.get, existingVanillaModelFile("block/dirt"))
    mkSimpleBlockItemModel(InfusedBasicBlocks.DIRT.get, existingVanillaModelFile("block/dirt"))

  }
}
