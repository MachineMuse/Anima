package net.machinemuse.anima
package bowl

import net.minecraft.block._
import net.minecraft.block.material.Material
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.shapes.{ISelectionContext, VoxelShape}
import net.minecraft.world.IBlockReader
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent

import registration.RegistryHelpers.regBlock
import util.DatagenHelpers.mkCenteredCuboidShape
import util.Logging

/**
 * Created by MachineMuse on 2/25/2021.
 */
object BowlInWorld extends Logging {
  @SubscribeEvent def onConstructMod(e: FMLConstructModEvent) = {}

  private[bowl] val SHAPE = mkCenteredCuboidShape(8, 4)

  private[bowl] val DATA_NAME = "bowl_placed"
  private[bowl] val BLOCKPROPERTIES = AbstractBlock.Properties.create(Material.WOOD)
    .hardnessAndResistance(2.0F).sound(SoundType.WOOD).notSolid

  // Block and BlockItem registration
  private[bowl] val BLOCK = regBlock(DATA_NAME, () => new BowlInWorld(BLOCKPROPERTIES))
}

@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
class BowlInWorld(properties: AbstractBlock.Properties) extends Block(properties) {
  import BowlInWorld._

  override def getShape(state : BlockState, world : IBlockReader, pos : BlockPos, ctx : ISelectionContext): VoxelShape = {
    SHAPE
  }

  override def hasTileEntity(state: BlockState): Boolean = true

  override def createTileEntity(state: BlockState, world: IBlockReader): TileEntity = {
    new BowlInWorldTileEntity
  }
}
