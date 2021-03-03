package net.machinemuse.anima
package salt

import net.minecraft.state.properties.RedstoneSide
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent

import util.DatagenHelpers.{PartBuilderWorkaround, existingVanillaModelFile, mkLanguageProvider, mkMultipartBlockStates}
import util.Logging

/**
 * Created by MachineMuse on 3/2/2021.
 */
@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
object SaltLineDatagen extends Logging {

  @SubscribeEvent def onGatherData(implicit event: GatherDataEvent): Unit = {
    mkLanguageProvider("en_us"){ lang =>
      // adds as a block i guess because it's an ItemBlock
      lang.addBlock(SaltLine.SALT_LINE_BLOCK, "Salt")
    }
    mkLanguageProvider("fr_fr"){ lang =>
      lang.addBlock(SaltLine.SALT_LINE_BLOCK, "Sel")
    }
    mkMultipartBlockStates(SaltLine.SALT_LINE_BLOCK.get){ builder =>
      val dotmodel = existingVanillaModelFile("block/redstone_dust_dot")
      val sidemodel = existingVanillaModelFile("block/redstone_dust_side")
      val sidemodel0 = existingVanillaModelFile("block/redstone_dust_side0")
      val sidemodel1 = existingVanillaModelFile("block/redstone_dust_side1")
      val sidealtmodel = existingVanillaModelFile("block/redstone_dust_side_alt")
      val sidealtmodel0 = existingVanillaModelFile("block/redstone_dust_side_alt0")
      val sidealtmodel1 = existingVanillaModelFile("block/redstone_dust_side_alt1")
      val upmodel = existingVanillaModelFile("block/redstone_dust_up")
      val sides = Seq(SaltLine.NORTH, SaltLine.EAST, SaltLine.SOUTH, SaltLine.WEST)
      val sidemodels = Seq(sidemodel0, sidealtmodel1, sidealtmodel0, sidemodel1)
      val siderotations = Seq(0, 270, 0, 270)
      for(sidenumber <- sides.indices) {
        builder.part.modelFile(upmodel).rotationY(sidenumber * 90).addModel()
          .saferCondition(sides(sidenumber), RedstoneSide.UP)
        builder.part.modelFile(sidemodels(sidenumber)).rotationY(siderotations(sidenumber)).addModel()
          .saferCondition(sides(sidenumber), RedstoneSide.SIDE, RedstoneSide.UP)
        builder.part.modelFile(dotmodel).addModel()
          .saferCondition(sides(sidenumber), RedstoneSide.UP, RedstoneSide.SIDE)
          .saferCondition(sides((sidenumber + 1) % 4), RedstoneSide.UP, RedstoneSide.SIDE)
      }
      builder.part.modelFile(dotmodel).addModel()
        .saferCondition(SaltLine.NORTH, RedstoneSide.NONE)
        .saferCondition(SaltLine.SOUTH, RedstoneSide.NONE)
        .saferCondition(SaltLine.EAST, RedstoneSide.NONE)
        .saferCondition(SaltLine.WEST, RedstoneSide.NONE)
    }

  }
}
