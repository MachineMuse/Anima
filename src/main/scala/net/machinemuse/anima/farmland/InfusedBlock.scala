package net.machinemuse.anima
package farmland

import net.minecraft.block._
import net.minecraft.state.{IntegerProperty, StateContainer}
import net.minecraft.util.Direction
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockReader
import net.minecraftforge.common.IPlantable
import org.apache.logging.log4j.scala.Logging

import util.VanillaClassEnrichers.RichBlockState

/**
 * Created by MachineMuse on 2/19/2021.
 */
object InfusedBlock extends Logging {

  // Nitrogen (cause nitrogen-fixing plants & fungi pull it in from the air)
  private[farmland] val INFUSED_AIR = IntegerProperty.create("infused_air", 0, 3)
  // Phosphorus (cause phosphorus-fixing fungi draw it from what's already in the soil but unavailable)
  private[farmland] val INFUSED_EARTH = IntegerProperty.create("infused_earth", 0, 3)
  // Potassium (cause it mostly comes from ash and is released in large quantities during forest fires)
  private[farmland] val INFUSED_FIRE = IntegerProperty.create("infused_fire", 0, 3)
  // Moisture
  private[farmland] val INFUSED_WATER = IntegerProperty.create("infused_water", 0, 3)
  // its just shade lol but abstracted
  private[farmland] val INFUSED_DARKNESS = IntegerProperty.create("infused_darkness", 0, 3)
  // Cold
  private[farmland] val INFUSED_COLD = IntegerProperty.create("infused_cold", 0, 3)

  // This increases if it doesn't have a consumed nutrient
  private[farmland] val DEPLETION = IntegerProperty.create("depletion", 0, 3)


  class InfusedBlock(val basis: Block, properties: AbstractBlock.Properties) extends Block(properties) with IInfusedBlock {
    setDefaultState(applyInfusionProperties(stateContainer.getBaseState))
  }

  trait IInfusedBlock extends Block with Logging {

    def applyInfusionProperties: BlockState => BlockState = {
      _.updated(INFUSED_AIR, 0)
        .updated(INFUSED_EARTH, 0)
        .updated(INFUSED_FIRE, 0)
        .updated(INFUSED_WATER, 0)
        .updated(INFUSED_DARKNESS, 0)
        .updated(INFUSED_COLD, 0)
        .updated(DEPLETION, 0)
    }

    override protected def fillStateContainer(builder: StateContainer.Builder[Block, BlockState]): Unit = {
      super.fillStateContainer(builder)
      builder.add(INFUSED_AIR)
      builder.add(INFUSED_EARTH)
      builder.add(INFUSED_FIRE)
      builder.add(INFUSED_WATER)
      builder.add(INFUSED_DARKNESS)
      builder.add(INFUSED_COLD)
      builder.add(DEPLETION)
    }

    override def canSustainPlant(thisState : BlockState, world : IBlockReader, pos : BlockPos, facing : Direction, plantable : IPlantable): Boolean = {
      val plantPos = pos.offset(facing)
      true
      //    if(plantable.getPlantType(world, plantPos) == PlantType.CROP) {
      //      true
      //    } else {
      //      false
      //    }
    }
  }

}