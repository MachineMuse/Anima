package net.machinemuse.anima
package entity

import net.minecraft.block.material.Material
import net.minecraft.block.{AbstractBlock, Block, BlockRenderType, BlockState}
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.shapes.{ISelectionContext, VoxelShape, VoxelShapes}
import net.minecraft.world.IBlockReader

/**
 * Created by MachineMuse on 1/25/2021.
 */
class AirLightBlock extends Block(AbstractBlock.Properties.create(Material.AIR).doesNotBlockMovement.noDrops.setLightLevel(_ => 15)) {

  override def getRenderType(state: BlockState): BlockRenderType = BlockRenderType.INVISIBLE

  override def getShape(state: BlockState, worldIn: IBlockReader, pos: BlockPos, context: ISelectionContext): VoxelShape = VoxelShapes.empty

}
