package net.machinemuse.anima
package campfire

import net.minecraft.block._
import net.minecraft.block.material.{Material, MaterialColor}
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.state.properties.BlockStateProperties
import net.minecraft.util.math.{BlockPos, BlockRayTraceResult}
import net.minecraft.util.{Unit => _, _}
import net.minecraft.world.{IBlockReader, World}
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent
import shapeless.HNil

import registration.RegistryHelpers._
import util.VanillaClassEnrichers.RichBlockState

/**
 * Created by MachineMuse on 1/24/2021.
 */

object CampfirePlus {
  // Init (Required for registration to work)
  // Use event.enqueueWork{ () => doStuff() } if you need to run any actual mutating code in here
  @SubscribeEvent def onConstructMod(e: FMLConstructModEvent) = {}


  private[campfire] val WATERLOGGED = BlockStateProperties.WATERLOGGED
  private[campfire] val SIGNAL_FIRE = BlockStateProperties.SIGNAL_FIRE
  private[campfire] val LIT = BlockStateProperties.LIT
  private[campfire] val HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING

  private[campfire] val DATA_NAME = "campfireplus"
  private[campfire] val BLOCKPROPERTIES = AbstractBlock.Properties.create(Material.WOOD, MaterialColor.OBSIDIAN)
    .hardnessAndResistance(2.0F).sound(SoundType.WOOD).notSolid
    .setLightLevel((state: BlockState) => if (state.get(BlockStateProperties.LIT).booleanValue()) 15 else 0)

  // Block and BlockItem registration
  private[campfire] val BLOCK = regBlock(DATA_NAME, () => new CampfirePlus(false, 1, BLOCKPROPERTIES))
  def getBlock = BLOCK.get
  private[campfire] val ITEM = regSimpleBlockItem(DATA_NAME, CampfirePlus.BLOCK)
  def getBlockItem = ITEM.get
}

@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
class CampfirePlus(smokey: Boolean, fireDamage: Int, properties: AbstractBlock.Properties) extends CampfireBlock(smokey, fireDamage, properties) {
  import CampfirePlus._
  override def createNewTileEntity(worldIn: IBlockReader) = new CampfirePlusTileEntity

  def getCopiedState(oldState: BlockState): BlockState = {
    this.getDefaultState.updated(
      (WATERLOGGED, oldState.get(WATERLOGGED)) ::
        (SIGNAL_FIRE, oldState.get(SIGNAL_FIRE)) ::
          (LIT, oldState.get(LIT)) ::
            (HORIZONTAL_FACING, oldState.get(HORIZONTAL_FACING)) :: HNil
    )
  }

  override def onBlockActivated(blockstate : BlockState, world : World, blockpos : BlockPos, playerentity : PlayerEntity, hand : Hand, blockRayTraceResult : BlockRayTraceResult): ActionResultType = {
    val heldItem = playerentity.getHeldItem(hand)
//    heldItem.getItem.optionallyAs[DyeItem].fold {
      // Item is not dye
      super.onBlockActivated(blockstate, world, blockpos, playerentity, hand, blockRayTraceResult)
//    } { dyeItem => // Item is dye!
//      world.getTileEntity(blockpos).optionallyDoAs[CampfirePlusTileEntity] { campfire =>
//        campfire.colour1 = campfire.colour2
//        campfire.colour2 = dyeItem.getDyeColor.getTextColor // Colour.mixColours(dyeItem.getDyeColor.getTextColor, campfire.colour, 1.0F/1.0F)
//      }
//      ActionResultType.SUCCESS
//    }
  }
}