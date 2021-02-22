package net.machinemuse.anima
package blockinfusion

import net.minecraft.block.Blocks
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent

import blockinfusion.BlockInfusionEvents.{ElementalBehaviour, SoilElement}
import util.DatagenHelpers._
import util.GenCodecsByName._
import util.Logging
import util.VanillaCodecs._

/**
 * Created by MachineMuse on 2/18/2021.
 */
@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
object BlockInfusionDatagen extends Logging {
  @SubscribeEvent def gatherData(implicit event: GatherDataEvent): Unit = {
    provideArbitrarySerializer[ElementalBehaviour]("crop_elements")(
      modLoc("beets") -> ElementalBehaviour(Blocks.BEETROOTS,
        infuses=List(SoilElement.Darkness), consumes = List(SoilElement.Earth),
        likes = List(SoilElement.Water), dislikes = List(SoilElement.Air)),

      modLoc("carrots") -> ElementalBehaviour(Blocks.CARROTS,
        infuses=List(SoilElement.Darkness), consumes = List(SoilElement.Earth),
        likes = List(SoilElement.Water), dislikes = List(SoilElement.Air)),

      modLoc("potatoes") -> ElementalBehaviour(Blocks.POTATOES,
        infuses=List(SoilElement.Darkness), consumes = List(SoilElement.Earth),
        likes = List(SoilElement.Water), dislikes = List(SoilElement.Air)),

      modLoc("wheat") -> ElementalBehaviour(Blocks.WHEAT,
        infuses=List(), consumes = List(SoilElement.Air),
        likes = List(), dislikes = List()),

      modLoc("cactus") -> ElementalBehaviour(Blocks.CACTUS,
        infuses=List(SoilElement.Fire), consumes = List(SoilElement.Earth),
        likes = List(SoilElement.Water), dislikes = List(SoilElement.Air)),

      modLoc("sugar_cane") -> ElementalBehaviour(Blocks.SUGAR_CANE,
        infuses=List(SoilElement.Air), consumes = List(SoilElement.Water),
        likes = List(SoilElement.Water), dislikes = List()),

      modLoc("melon") -> ElementalBehaviour(Blocks.MELON_STEM,
        infuses=List(SoilElement.Darkness), consumes = List(SoilElement.Air),
        likes = List(SoilElement.Water), dislikes = List(SoilElement.Air)),

      modLoc("pumpkin") -> ElementalBehaviour(Blocks.PUMPKIN_STEM,
        infuses=List(SoilElement.Darkness), consumes = List(SoilElement.Air),
        likes = List(SoilElement.Water), dislikes = List(SoilElement.Air)),
    )


  }
}
