package net.machinemuse.anima
package salt

import com.google.common.collect.Maps
import net.minecraft.block.Block.{replaceBlockState, spawnDrops}
import net.minecraft.block._
import net.minecraft.block.material.{Material, MaterialColor}
import net.minecraft.client.renderer.{RenderType, RenderTypeLookup}
import net.minecraft.entity.{LivingEntity, MobEntity}
import net.minecraft.item.BlockItemUseContext
import net.minecraft.pathfinding.PathNodeType
import net.minecraft.state.properties.{BlockStateProperties, RedstoneSide}
import net.minecraft.state.{EnumProperty, StateContainer}
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.shapes._
import net.minecraft.util.{Direction, Rotation}
import net.minecraft.world._
import net.minecraftforge.api.distmarker.{Dist, OnlyIn}
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle._
import shapeless.HNil

import scala.annotation.nowarn
import scala.jdk.CollectionConverters.ListHasAsScala

import registration.RegistryHelpers.regBlock
import util.Logging
import util.VanillaClassEnrichers._

/**
 * Created by MachineMuse on 2/10/2021.
 */
object SaltLine extends Logging {

  @SubscribeEvent def onConstructMod(e: FMLConstructModEvent) = {}
  private[salt] val NORTH = BlockStateProperties.REDSTONE_NORTH
  private[salt] val EAST = BlockStateProperties.REDSTONE_EAST
  private[salt] val SOUTH = BlockStateProperties.REDSTONE_SOUTH
  private[salt] val WEST = BlockStateProperties.REDSTONE_WEST

  private val BASE_SHAPE: VoxelShape = Block.makeCuboidShape(3.0D, 0.0D, 3.0D, 13.0D, 1.0D, 13.0D)

  private def propertyOfDirection: Direction => EnumProperty[RedstoneSide] = {
    case Direction.NORTH => NORTH
    case Direction.EAST  => EAST
    case Direction.SOUTH => SOUTH
    case Direction.WEST  => WEST
    case _ => ??? // we aren't supposed to use this this way...
  }

  private val HORIZONTAL_DIRECTIONS = Seq(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)
  private val VERTICAL_DIRECTIONS = Seq(Direction.UP, Direction.DOWN)

  private val SHAPE_FOR_UNDEAD: VoxelShape = Block.makeCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 128.0D, 16.0D)

  val properties = AbstractBlock.Properties.create(Material.MISCELLANEOUS, MaterialColor.WHITE_TERRACOTTA).zeroHardnessAndResistance().notSolid()
    .setBlocksVision((_,_,_) => false).setSuffocates((_,_,_) => false).doesNotBlockMovement
  val SALT_LINE_BLOCK = regBlock("salt_line", () => new SaltLine(properties))


  implicit class RedstoneSideHelper(side: RedstoneSide) {
    def notNone = side != RedstoneSide.NONE
  }

  @OnlyIn(Dist.CLIENT)
  @SubscribeEvent def onClientSetup(event: FMLClientSetupEvent) = {
    RenderTypeLookup.setRenderLayer(SALT_LINE_BLOCK.get, RenderType.getCutout)
  }

}

@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
class SaltLine(properties: AbstractBlock.Properties) extends Block(properties) {
  import SaltLine._

  // Constructor
  this.setDefaultState(this.stateContainer.getBaseState.updated(
                                        NORTH -> RedstoneSide.NONE ::
                                        EAST -> RedstoneSide.NONE ::
                                        SOUTH  -> RedstoneSide.NONE ::
                                        WEST  ->  RedstoneSide.NONE :: HNil))
  private val stateToShapeMap: java.util.Map[BlockState, VoxelShape] = Maps.newHashMap

  { // stuff that's only used in the constructor
    val SIDE_TO_SHAPE: Map[Direction, VoxelShape] = Map(
      Direction.NORTH -> Block.makeCuboidShape(3.0D, 0.0D, 0.0D, 13.0D, 1.0D, 13.0D),
      Direction.SOUTH -> Block.makeCuboidShape(3.0D, 0.0D, 3.0D, 13.0D, 1.0D, 16.0D),
      Direction.EAST -> Block.makeCuboidShape(3.0D, 0.0D, 3.0D, 16.0D, 1.0D, 13.0D),
      Direction.WEST -> Block.makeCuboidShape(0.0D, 0.0D, 3.0D, 13.0D, 1.0D, 13.0D))
    val SIDE_TO_ASCENDING_SHAPE: Map[Direction, VoxelShape] = Map(
      Direction.NORTH -> VoxelShapes.or(SIDE_TO_SHAPE(Direction.NORTH), Block.makeCuboidShape(3.0D, 0.0D, 0.0D, 13.0D, 16.0D, 1.0D)),
      Direction.SOUTH -> VoxelShapes.or(SIDE_TO_SHAPE(Direction.SOUTH), Block.makeCuboidShape(3.0D, 0.0D, 15.0D, 13.0D, 16.0D, 16.0D)),
      Direction.EAST -> VoxelShapes.or(SIDE_TO_SHAPE(Direction.EAST), Block.makeCuboidShape(15.0D, 0.0D, 3.0D, 16.0D, 16.0D, 13.0D)),
      Direction.WEST -> VoxelShapes.or(SIDE_TO_SHAPE(Direction.WEST), Block.makeCuboidShape(0.0D, 0.0D, 3.0D, 1.0D, 16.0D, 13.0D)))
    def getShapeForState(state: BlockState) =
      HORIZONTAL_DIRECTIONS.foldLeft(BASE_SHAPE) { case (voxelshape, direction) =>
        state.get(propertyOfDirection(direction)) match {
          case RedstoneSide.SIDE => VoxelShapes.or(voxelshape, SIDE_TO_SHAPE(direction))
          case RedstoneSide.UP => VoxelShapes.or(voxelshape, SIDE_TO_ASCENDING_SHAPE(direction))
          case _ => voxelshape
        }
      }
    for (blockstate <- this.getStateContainer.getValidStates.asScala) {
      this.stateToShapeMap.put(blockstate, getShapeForState(blockstate))
    }
  }

  // End Constructor

  // TODO: Come back to this when PR is approved https://github.com/MinecraftForge/MinecraftForge/pull/7655
  override def getAiPathNodeType(state: BlockState, world: IBlockReader, pos: BlockPos, entity: MobEntity): PathNodeType =
    if(entity != null && entity.isEntityUndead) {
      // never actually happens because of #7655
      PathNodeType.BLOCKED
    } else {
      // a compromise for now
      PathNodeType.RAIL
    }


  override def rotate(state: BlockState, rot: Rotation): BlockState = {
    rot match {
      case Rotation.CLOCKWISE_180 =>
        state.updated(
          NORTH -> state.get(SOUTH) ::
          EAST -> state.get(SOUTH) ::
          SOUTH  -> state.get(WEST) ::
          WEST  ->  state.get(EAST) :: HNil)
      case Rotation.COUNTERCLOCKWISE_90 =>
        state.updated(
          NORTH -> state.get(EAST) ::
          EAST -> state.get(SOUTH) ::
          SOUTH  -> state.get(WEST) ::
          WEST  ->  state.get(NORTH) :: HNil)
      case Rotation.CLOCKWISE_90 =>
        state.updated(
          NORTH -> state.get(WEST) ::
          EAST -> state.get(NORTH) ::
          SOUTH  -> state.get(EAST) ::
          WEST  ->  state.get(SOUTH) :: HNil)
      case _ =>
        state
    }
  }

  override def getShape(state: BlockState, worldIn: IBlockReader, pos: BlockPos, context: ISelectionContext): VoxelShape = {
    this.stateToShapeMap.get(state)
  }

  override def getCollisionShape(state: BlockState, worldIn: IBlockReader, pos: BlockPos, context: ISelectionContext): VoxelShape =
    context.getEntity match {
      case undead: LivingEntity if undead.isEntityUndead => SHAPE_FOR_UNDEAD
      case _ => stateToShapeMap.get(state)
    }

  override def getStateForPlacement(context: BlockItemUseContext): BlockState = {
    recalculateState(context.getWorld, context.getPos)
  }

  override def updatePostPlacement(state: BlockState, directionChanged: Direction, facingState: BlockState, world: IWorld, pos: BlockPos, facingPos: BlockPos): BlockState = {
    if (directionChanged == Direction.DOWN) {
      state
    } else if (directionChanged == Direction.UP) {
      this.recalculateState(world, pos)
    } else {
      val redstoneside = this.recalculateSide(world, pos, directionChanged, !world.getBlockState(pos.up).isNormalCube(world, pos))
      if (redstoneside.notNone == state.get(propertyOfDirection(directionChanged)).notNone && !areAllSidesNotNone(state)) {
        state.updated(propertyOfDirection(directionChanged), redstoneside)
      } else {
        this.recalculateState(world, pos)
      }
    }
  }

  override def updateDiagonalNeighbors(state: BlockState, world: IWorld, pos: BlockPos, flags: Int, recursionLeft: Int): Unit = {
    for {horizontalDirection <- HORIZONTAL_DIRECTIONS
         verticalDirection <- VERTICAL_DIRECTIONS} {
      val adjacent = pos.offset(horizontalDirection)
      if (state.get(propertyOfDirection(horizontalDirection)).notNone && !world.getBlockState(adjacent).isIn(SALT_LINE_BLOCK.get)) {
        val diagonal = adjacent.offset(verticalDirection)
        val oldStateDiagonal = world.getBlockState(diagonal)
        if (!oldStateDiagonal.isIn(Blocks.OBSERVER)) {
          val verticallyAdjacent = diagonal.offset(horizontalDirection.getOpposite)
          val newStateDiagonal = oldStateDiagonal.updatePostPlacement(horizontalDirection.getOpposite, world.getBlockState(verticallyAdjacent), world, diagonal, verticallyAdjacent)
          replaceBlockState(oldStateDiagonal, newStateDiagonal, world, diagonal, flags, recursionLeft)
        }
      }
    }
  }

  override def isValidPosition(state: BlockState, world: IWorldReader, pos: BlockPos): Boolean = {
    this.canPlaceOnTopOf(world, pos.down, world.getBlockState(pos.down))
  }

  override def onBlockAdded(state: BlockState, world: World, pos: BlockPos, oldState: BlockState, isMoving: Boolean): Unit = {
    if (!oldState.isIn(state.getBlock) && !world.isRemote) {
      for (direction <- VERTICAL_DIRECTIONS) {
        world.notifyNeighborsOfStateChange(pos.offset(direction), this)
      }
      this.updateNeighboursStateChange(world, pos)
    }
  }

  override def onReplaced(state: BlockState, world: World, pos: BlockPos, newState: BlockState, isMoving: Boolean): Unit = {
    if (!isMoving && !state.isIn(newState.getBlock)) {
      super.onReplaced(state, world, pos, newState, isMoving)
      world.onServer { serverWorld =>
        for (direction <- Direction.values) {
          serverWorld.notifyNeighborsOfStateChange(pos.offset(direction), this)
        }
        this.updateNeighboursStateChange(serverWorld, pos)
      }
    }
  } : @nowarn

  override def neighborChanged(state: BlockState, world: World, pos: BlockPos, blockIn: Block, fromPos: BlockPos, isMoving: Boolean): Unit = {
    if (!world.isRemote) {
      if (!state.isValidPosition(world, pos)) {
        spawnDrops(state, world, pos)
        world.removeBlock(pos, false)
      }
    }
  }

  override def fillStateContainer(builder: StateContainer.Builder[Block, BlockState]): Unit = {
    builder.add(NORTH, EAST, SOUTH, WEST)
  }

  private def recalculateState(reader: IBlockReader, pos: BlockPos) = {
    val newstate = this.recalculateSides(reader, pos)
    val sideStates = getAllSides(newstate)
    val numUnconnected = sideStates.count(_ == RedstoneSide.NONE)
    numUnconnected match {
      case 3 =>
        val sides = Seq(NORTH, EAST, SOUTH, WEST)
        val index = sideStates.indexWhere(_ != RedstoneSide.NONE)
        newstate.updated(sides((index + 2)%4), RedstoneSide.SIDE)
      case _ => newstate
    }
  }

  private def recalculateSides(reader: IBlockReader, pos: BlockPos) = {
    val notNormalCubeAbove = !reader.getBlockState(pos.up).isNormalCube(reader, pos)
    HORIZONTAL_DIRECTIONS.foldLeft(getDefaultState) { (newstate, direction) =>
      val directionProperty = propertyOfDirection(direction)
      newstate.updated(directionProperty, this.recalculateSide(reader, pos, direction, notNormalCubeAbove))
    }
  }

  private def areAllSidesNotNone(state: BlockState) = {
    state.get(NORTH) != RedstoneSide.NONE &&
      state.get(SOUTH) != RedstoneSide.NONE &&
      state.get(EAST) != RedstoneSide.NONE &&
      state.get(WEST) != RedstoneSide.NONE
  }

  private def getAllSides(state: BlockState) = {
    Seq(state.get(NORTH), state.get(EAST), state.get(SOUTH), state.get(WEST))
  }

  private def recalculateSide(world: IBlockReader, pos: BlockPos, direction: Direction, nonNormalCubeAbove: Boolean): RedstoneSide = {
    val sidepos = pos.offset(direction)
    val sidestate = world.getBlockState(sidepos)
    if (nonNormalCubeAbove &&
      canPlaceOnTopOf(world, sidepos, sidestate) &&
      canConnectTo(world.getBlockState(sidepos.up), world, sidepos.up, null)
    ) {
      if (sidestate.isSolidSide(world, sidepos, direction.getOpposite))
        RedstoneSide.UP
      else
        RedstoneSide.SIDE
    } else {
      if (!canConnectTo(sidestate, world, sidepos, direction) &&
        (sidestate.isNormalCube(world, sidepos) || !canConnectTo(world.getBlockState(sidepos.down), world, sidepos.down, null))
      )
        RedstoneSide.NONE
      else
        RedstoneSide.SIDE
    }
  }

  protected def canConnectTo(blockState: BlockState, world: IBlockReader, pos: BlockPos, side: Direction): Boolean = {
    blockState.isIn(SALT_LINE_BLOCK.get)
  }

  private def canPlaceOnTopOf(reader: IBlockReader, pos: BlockPos, state: BlockState) = {
    state.isSolidSide(reader, pos, Direction.UP) || state.isIn(Blocks.HOPPER)
  }

  private def updateNeighboursStateChange(world: World, pos: BlockPos): Unit = {
    for (direction <- HORIZONTAL_DIRECTIONS) {
      this.notifySaltNeighborsOfStateChange(world, pos.offset(direction))
    }
    for (direction <- HORIZONTAL_DIRECTIONS) {
      val blockpos = pos.offset(direction)
      if (world.getBlockState(blockpos).isNormalCube(world, blockpos)){
        this.notifySaltNeighborsOfStateChange(world, blockpos.up)
      } else {
        this.notifySaltNeighborsOfStateChange(world, blockpos.down)
      }
    }
  }


  private def notifySaltNeighborsOfStateChange(world: World, pos: BlockPos): Unit = {
    if (world.getBlockState(pos).isIn(SALT_LINE_BLOCK.get)) {
      world.notifyNeighborsOfStateChange(pos, SALT_LINE_BLOCK.get)
      for (direction <- Direction.values) {
        world.notifyNeighborsOfStateChange(pos.offset(direction), SALT_LINE_BLOCK.get)
      }
    }
  }
}
