package net.machinemuse.anima
package catstatue

import net.minecraft.block._
import net.minecraft.block.material.{Material, MaterialColor}
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item._
import net.minecraft.state.properties.BlockStateProperties
import net.minecraft.state.{DirectionProperty, StateContainer}
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.shapes.{ISelectionContext, VoxelShape}
import net.minecraft.util.math.{BlockPos, BlockRayTraceResult}
import net.minecraft.util.{Unit => _, _}
import net.minecraft.world.{IBlockReader, World}
import net.minecraftforge.common.ForgeHooks
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent

import scala.annotation.nowarn

import bowl.BowlWithContents
import registration.RegistryHelpers.{regBlock, regSimpleBlockItem}
import util.Logging
import util.VanillaClassEnrichers.RichBlockState


/**
 * Created by MachineMuse on 2/11/2021.
 */
object CatStatue extends Logging {
  @SubscribeEvent def onConstructMod(e: FMLConstructModEvent) = {}

  private val properties = AbstractBlock.Properties.create(Material.CLAY, MaterialColor.RED)
    .hardnessAndResistance(2.0F).sound(SoundType.BONE).notSolid

  private[catstatue] val BLOCK = regBlock("cat_statue", () => new CatStatue(properties))
  private[catstatue] val ITEM = regSimpleBlockItem("cat_statue", BLOCK)

  private[catstatue] val HORIZONTAL_DIRECTIONS = List(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)


  val FACING: DirectionProperty = BlockStateProperties.HORIZONTAL_FACING
  val WATERLEVEL = BlockStateProperties.LEVEL_0_3
  val LIT = BlockStateProperties.LIT

  protected val SHAPENORMAL = Block.makeCuboidShape(5.0D, 0.0D, 3.0D, 11.0D, 16.0D, 13.0D)
  protected val SHAPESIDE = Block.makeCuboidShape(3.0D, 0.0D, 5.0D, 13.0D, 16.0D, 11.0D)
}

@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
class CatStatue(properties: AbstractBlock.Properties) extends Block(properties) with Logging {
  import CatStatue._
  setDefaultState(this.stateContainer.getBaseState.updated(WATERLEVEL,0).updated(FACING, Direction.NORTH))

  override def getShape(state: BlockState, worldIn: IBlockReader, pos: BlockPos, context: ISelectionContext): VoxelShape = {
    state.get(FACING) match {
      case Direction.NORTH | Direction.SOUTH => SHAPENORMAL
      case Direction.WEST | Direction.EAST => SHAPESIDE
      case Direction.DOWN | Direction.UP => ???
    }
  }

  override def onBlockActivated(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, hit: BlockRayTraceResult): ActionResultType = {
//    hit.getFace match {
    val itemInUse = player.getHeldItem(hand)
    if(itemInUse.getItem == BowlWithContents.BOWL_OF_WATER.get) {
      val waterLevel = state.get(WATERLEVEL)
      if (waterLevel < 3) {
        val emptyBowl = new ItemStack(Items.BOWL)
        val result = DrinkHelper.fill(itemInUse, player, emptyBowl, false)
        player.setHeldItem(hand, result)
        world.setBlockState(pos, state.updated(WATERLEVEL, waterLevel + 1))
        ActionResultType.SUCCESS
      } else {
        ActionResultType.PASS
      }
    } else if (ForgeHooks.getBurnTime(itemInUse) > 0) {
      world.getTileEntity(pos).mapAsOrElse[CatStatueTileEntity, ActionResultType](ActionResultType.PASS) { tileEntity =>
        tileEntity.addFuel(ForgeHooks.getBurnTime(itemInUse))
        val container = itemInUse.getContainerItem
        val result = DrinkHelper.fill(itemInUse, player, container, false)
        player.setHeldItem(hand, result)
        ActionResultType.SUCCESS
      }
    } else {
      ActionResultType.PASS
    }
  }

  override def createTileEntity(state: BlockState, world: IBlockReader): TileEntity = new CatStatueTileEntity

  override def hasTileEntity(state: BlockState): Boolean = true

  override def getStateForPlacement(context: BlockItemUseContext): BlockState = {
    // Face towards the player
    getDefaultState.updated(FACING, context.getPlacementHorizontalFacing.getOpposite)
  }

  override def rotate(state: BlockState, rot: Rotation): BlockState = state.`with`(FACING, rot.rotate(state.get(FACING))): @nowarn
  override def mirror(state: BlockState, mirrorIn: Mirror): BlockState = state.rotate(mirrorIn.toRotation(state.get(FACING))): @nowarn

  override def fillStateContainer(builder: StateContainer.Builder[Block, BlockState]) = {
    builder.add(WATERLEVEL, FACING, LIT)
  }
}
