package net.machinemuse.anima
package claypot

import net.minecraft.block._
import net.minecraft.block.material.{Material, MaterialColor}
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.BlockItemUseContext
import net.minecraft.state._
import net.minecraft.state.properties.BlockStateProperties
import net.minecraft.util.math.shapes.{ISelectionContext, VoxelShape}
import net.minecraft.util.math.{BlockPos, BlockRayTraceResult}
import net.minecraft.util.{Unit => _, _}
import net.minecraft.world.{IBlockReader, World}
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent

import scala.annotation.nowarn

import constants.BlockStateFlags
import registration.RegistryHelpers.{regBlock, regSimpleBlockItem}
import util.Logging

/**
 * Created by MachineMuse on 2/9/2021.
 */
object ClayPotBlock extends Logging {

  @SubscribeEvent def onConstructMod(e: FMLConstructModEvent) = {}
  private val properties = AbstractBlock.Properties.create(Material.CLAY, MaterialColor.CLAY)
    .hardnessAndResistance(2.0F).sound(SoundType.BONE).notSolid
  private[claypot] val BLOCK = regBlock("clay_pot", () => new ClayPotBlock(properties))
  private[claypot] val ITEM = regSimpleBlockItem("clay_pot", BLOCK)

  private[claypot] val OPEN: BooleanProperty = BlockStateProperties.OPEN
  private[claypot] val FACING: DirectionProperty = BlockStateProperties.HORIZONTAL_FACING

  protected val SHAPE = Block.makeCuboidShape(1.0D, 0.0D, 1.0D, 15.0D, 12.0D, 15.0D)

}

@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
class ClayPotBlock(properties: AbstractBlock.Properties) extends Block(properties) with Logging {
  import ClayPotBlock._
  setDefaultState(this.stateContainer.getBaseState.`with`(OPEN, Boolean.box(false)).`with`(FACING, Direction.NORTH))

  override def getShape(state: BlockState, worldIn: IBlockReader, pos: BlockPos, context: ISelectionContext): VoxelShape = SHAPE

  override def onBlockActivated(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, handIn: Hand, hit: BlockRayTraceResult): ActionResultType = {
    hit.getFace match {
      case Direction.UP => {
        val open = state.get(OPEN)
        world.setBlockState(pos, state.`with`(OPEN, Boolean.box(!open)), BlockStateFlags.STANDARD_CLIENT_UPDATE)
        ActionResultType.SUCCESS
      }
      case _ => ActionResultType.PASS
    }
  }

  override def getStateForPlacement(context: BlockItemUseContext): BlockState = this.getDefaultState.`with`(FACING, context.getPlacementHorizontalFacing)

  override def rotate(state: BlockState, rot: Rotation): BlockState = state.`with`(FACING, rot.rotate(state.get(FACING))): @nowarn
  override def mirror(state: BlockState, mirrorIn: Mirror): BlockState = state.rotate(mirrorIn.toRotation(state.get(FACING))): @nowarn

  override def fillStateContainer(builder: StateContainer.Builder[Block, BlockState]) = {
    builder.add(OPEN, FACING)
  }
}
