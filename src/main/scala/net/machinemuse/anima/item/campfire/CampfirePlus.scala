package net.machinemuse.anima
package item
package campfire

import net.machinemuse.anima.item.campfire.CampfirePlus._
import net.minecraft.block._
import net.minecraft.block.material.{Material, MaterialColor}
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.DyeItem
import net.minecraft.state.Property
import net.minecraft.state.properties.BlockStateProperties
import net.minecraft.util.math.{BlockPos, BlockRayTraceResult}
import net.minecraft.util.{Unit => _, _}
import net.minecraft.world.{IBlockReader, World}

import scala.jdk.OptionConverters._

/**
 * Created by MachineMuse on 1/24/2021.
 */

object CampfirePlus {

  val smokey = false
  val fireDamage = 1
  val properties = AbstractBlock.Properties.create(Material.WOOD, MaterialColor.OBSIDIAN)
    .hardnessAndResistance(2.0F)
    .sound(SoundType.WOOD)
    .setLightLevel((state: BlockState) => if (state.get(BlockStateProperties.LIT)) 15 else 0)
    .notSolid
}

class CampfirePlus extends CampfireBlock(smokey, fireDamage, properties) {

  override def createNewTileEntity(worldIn: IBlockReader) = new CampfirePlusTileEntity


  def getCopiedState(oldState: BlockState): BlockState = {
    // this function gets the property from the old state, and if it exists, overwrites the new state with the old property.
    def apply[T <: Comparable[T]](prop: Property[T]): BlockState => BlockState = oldState.func_235903_d_(prop).toScala.foldId(_.`with`(prop, _))

    val f = apply(BlockStateProperties.WATERLOGGED) andThen apply(BlockStateProperties.SIGNAL_FIRE) andThen apply(BlockStateProperties.LIT) andThen apply(BlockStateProperties.HORIZONTAL_FACING)
    f(this.getDefaultState)
  }

  override def onBlockActivated(blockstate : BlockState, world : World, blockpos : BlockPos, playerentity : PlayerEntity, hand : Hand, blockRayTraceResult : BlockRayTraceResult): ActionResultType = {
    val heldItem = playerentity.getHeldItem(hand)
    heldItem.getItem.optionallyAs[DyeItem].fold {
      // Item is not dye
      super.onBlockActivated(blockstate, world, blockpos, playerentity, hand, blockRayTraceResult)
    } { dyeItem => // Item is dye!
      world.getTileEntity(blockpos).optionallyDoAs[CampfirePlusTileEntity] { campfire =>
        campfire.colour1 = campfire.colour2
        campfire.colour2 = dyeItem.getDyeColor.getTextColor // Colour.mixColours(dyeItem.getDyeColor.getTextColor, campfire.colour, 1.0F/1.0F)
      }
      ActionResultType.SUCCESS
    }
  }
}