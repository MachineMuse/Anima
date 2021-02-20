package net.machinemuse.anima
package farmland

import net.minecraft.block.Block.nudgeEntitiesWithNewState
import net.minecraft.block._
import net.minecraft.block.material.Material
import net.minecraft.entity.Entity
import net.minecraft.item.BlockItemUseContext
import net.minecraft.pathfinding.PathType
import net.minecraft.util.Direction
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.shapes.{ISelectionContext, VoxelShape}
import net.minecraft.world.server.ServerWorld
import net.minecraft.world._
import net.minecraftforge.common.ForgeHooks
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent
import org.apache.logging.log4j.scala.Logging

import java.util.Random
import scala.annotation.nowarn

import farmland.InfusedBlock.InfusedBlock
import registration.RegistryHelpers.{regBlock, regSimpleBlockItem}

/**
 * Created by MachineMuse on 2/17/2021.
 */
object InfusedFarmland extends Logging {
  @SubscribeEvent def onConstructMod(e: FMLConstructModEvent) = {}

  protected val SHAPE: VoxelShape = Block.makeCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 15.0D, 16.0D)

  private[farmland] val properties = AbstractBlock.Properties.create(Material.EARTH)
    .tickRandomly
    .hardnessAndResistance(0.6F)
    .sound(SoundType.GROUND)
    .setBlocksVision((state, world, pos) => true)
    .setSuffocates((state, world, pos) => true)
  private[farmland] val BLOCK = regBlock("infused_farmland", () => new InfusedFarmland(properties))
  private[farmland] val ITEM = regSimpleBlockItem("infused_farmland", BLOCK)

}

@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
class InfusedFarmland(properties: AbstractBlock.Properties) extends InfusedBlock(Blocks.FARMLAND, properties) with Logging {
  import InfusedFarmland._

  override def updatePostPlacement(stateIn: BlockState, facing: Direction, facingState: BlockState, worldIn: IWorld, currentPos: BlockPos, facingPos: BlockPos): BlockState = {
    if ((facing == Direction.UP) && !stateIn.isValidPosition(worldIn, currentPos)) {
      worldIn.getPendingBlockTicks.scheduleTick(currentPos, this, 1)
    }
    super.updatePostPlacement(stateIn, facing, facingState, worldIn, currentPos, facingPos) : @nowarn
  }

  override def isValidPosition(state: BlockState, worldIn: IWorldReader, pos: BlockPos): Boolean = {
    val blockstate = worldIn.getBlockState(pos.up)
    !blockstate.getMaterial.isSolid || blockstate.getBlock.isInstanceOf[FenceGateBlock] || blockstate.getBlock.isInstanceOf[MovingPistonBlock]
  }

  override def getStateForPlacement(context: BlockItemUseContext): BlockState =
    if (!this.getDefaultState.isValidPosition(context.getWorld, context.getPos)) {
      Blocks.DIRT.getDefaultState
    } else {
      super.getStateForPlacement(context)
    }

  override def isTransparent(state: BlockState) = true

  override def getShape(state: BlockState, worldIn: IBlockReader, pos: BlockPos, context: ISelectionContext): VoxelShape = SHAPE

  override def tick(state: BlockState, worldIn: ServerWorld, pos: BlockPos, rand: Random): Unit = {
    if (!state.isValidPosition(worldIn, pos)) turnToDirt(state, worldIn, pos)
  }

  /**
   * Block's chance to react to a living entity falling on it.
   */
  override def onFallenUpon(worldIn: World, pos: BlockPos, entityIn: Entity, fallDistance: Float): Unit = {
    if (!worldIn.isRemote && ForgeHooks.onFarmlandTrample(worldIn, pos, InfusedBasicBlocks.DIRT.get.getDefaultState, fallDistance, entityIn)) { // Forge: Move logic to Entity#canTrample
      turnToDirt(worldIn.getBlockState(pos), worldIn, pos)
    }
    super.onFallenUpon(worldIn, pos, entityIn, fallDistance)
  }

  def turnToDirt(state: BlockState, worldIn: World, pos: BlockPos): Unit = {
    worldIn.setBlockState(pos, nudgeEntitiesWithNewState(state, InfusedBasicBlocks.DIRT.get.getDefaultState, worldIn, pos))
  }

  override def allowsMovement(state: BlockState, worldIn: IBlockReader, pos: BlockPos, `type`: PathType) = false
}
