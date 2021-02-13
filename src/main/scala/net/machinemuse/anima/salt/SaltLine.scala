package net.machinemuse.anima
package salt

import registration.RegistryHelpers.regBlock
import util.DatagenHelpers._
import util.VanillaClassEnrichers._

import com.google.common.collect.Maps
import net.minecraft.block.Block.{replaceBlockState, spawnDrops}
import net.minecraft.block._
import net.minecraft.block.material.Material
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
import org.apache.logging.log4j.scala.Logging
import shapeless.HNil

import scala.annotation.nowarn
import scala.jdk.CollectionConverters.ListHasAsScala

/**
 * Created by MachineMuse on 2/10/2021.
 */
object SaltLine extends Logging {

  @SubscribeEvent def onConstructMod(e: FMLConstructModEvent) = {}
  private val NORTH = BlockStateProperties.REDSTONE_NORTH
  private val EAST = BlockStateProperties.REDSTONE_EAST
  private val SOUTH = BlockStateProperties.REDSTONE_SOUTH
  private val WEST = BlockStateProperties.REDSTONE_WEST

  private val BASE_SHAPE: VoxelShape = Block.makeCuboidShape(3.0D, 0.0D, 3.0D, 13.0D, 1.0D, 13.0D)

  private def propertyOfDirection: Direction => EnumProperty[RedstoneSide] = {
    case Direction.NORTH => NORTH
    case Direction.EAST  => EAST
    case Direction.SOUTH => SOUTH
    case Direction.WEST  => WEST
    case _ => ??? // we aren't supposed to use this this way...
  }

  private val HORIZONTAL_DIRECTIONS = List(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)
  private val VERTICAL_DIRECTIONS = List(Direction.UP, Direction.DOWN)

  private val SHAPE_FOR_UNDEAD: VoxelShape = Block.makeCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 128.0D, 16.0D)

  val properties = AbstractBlock.Properties.create(Material.MISCELLANEOUS).zeroHardnessAndResistance().notSolid()
    .setBlocksVision((_,_,_) => false).setSuffocates((_,_,_) => false).doesNotBlockMovement
  val SALT_LINE_BLOCK = regBlock("salt_line", () => new SaltLine(properties))


  implicit class RedstoneSideHelper(side: RedstoneSide) {
    def notNone = side != RedstoneSide.NONE
  }

  @OnlyIn(Dist.CLIENT)
  @SubscribeEvent def onClientSetup(event: FMLClientSetupEvent) = {
    RenderTypeLookup.setRenderLayer(SALT_LINE_BLOCK.get, RenderType.getCutout)
  }

  @SubscribeEvent def onGatherData(implicit event: GatherDataEvent): Unit = {
    mkLanguageProvider("en_us"){ lang =>
      // adds as a block i guess because it's an ItemBlock
      lang.addBlock(SALT_LINE_BLOCK, "Salt")
    }
    mkLanguageProvider("fr_fr"){ lang =>
      lang.addBlock(SALT_LINE_BLOCK, "Sel")
    }
    mkMultipartBlockStates(SALT_LINE_BLOCK.get){ builder =>
      val dotmodel = existingVanillaModelFile("block/redstone_dust_dot")
      val sidemodel = existingVanillaModelFile("block/redstone_dust_side")
      val sidemodel0 = existingVanillaModelFile("block/redstone_dust_side0")
      val sidemodel1 = existingVanillaModelFile("block/redstone_dust_side1")
      val sidealtmodel = existingVanillaModelFile("block/redstone_dust_side_alt")
      val sidealtmodel0 = existingVanillaModelFile("block/redstone_dust_side_alt0")
      val sidealtmodel1 = existingVanillaModelFile("block/redstone_dust_side_alt1")
      val upmodel = existingVanillaModelFile("block/redstone_dust_up")
      val sides = List(NORTH, EAST, SOUTH, WEST)
      val sidemodels = List(sidemodel0, sidealtmodel1, sidealtmodel0, sidemodel1)
      val siderotations = List(0, 270, 0, 270)
      for(sidenumber <- sides.indices) {
        builder.part.modelFile(upmodel).rotationY(sidenumber * 90).addModel()
          .saferCondition(sides(sidenumber), RedstoneSide.UP)
        builder.part.modelFile(sidemodels(sidenumber)).rotationY(siderotations(sidenumber)).addModel()
          .saferCondition(sides(sidenumber), RedstoneSide.SIDE, RedstoneSide.UP)
        builder.part.modelFile(dotmodel).addModel()
          .saferCondition(sides(sidenumber), RedstoneSide.UP, RedstoneSide.SIDE)
          .saferCondition(sides((sidenumber + 1) % 4), RedstoneSide.UP, RedstoneSide.SIDE)
      }
      builder.part.modelFile(dotmodel).addModel()
        .saferCondition(NORTH, RedstoneSide.NONE)
        .saferCondition(SOUTH, RedstoneSide.NONE)
        .saferCondition(EAST, RedstoneSide.NONE)
        .saferCondition(WEST, RedstoneSide.NONE)
    }

  }
}

@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
class SaltLine(properties: AbstractBlock.Properties) extends Block(properties) with Logging {
  import SaltLine._

  // Constructor
  this.setDefaultState(this.stateContainer.getBaseState.updated(
                                        NORTH -> RedstoneSide.NONE ::
                                        EAST -> RedstoneSide.NONE ::
                                        SOUTH  -> RedstoneSide.NONE ::
                                        WEST  ->  RedstoneSide.NONE :: HNil))
  private val stateToShapeMap: java.util.Map[BlockState, VoxelShape] = Maps.newHashMap
  private val sideBaseState = this.getDefaultState.updated(
                                        NORTH -> RedstoneSide.SIDE ::
                                        EAST -> RedstoneSide.SIDE ::
                                        SOUTH -> RedstoneSide.SIDE ::
                                        WEST -> RedstoneSide.SIDE :: HNil)

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
    getUpdatedState(context.getWorld, sideBaseState, context.getPos)
  }

  override def updatePostPlacement(state: BlockState, directionChanged: Direction, facingState: BlockState, worldIn: IWorld, currentPos: BlockPos, facingPos: BlockPos): BlockState = {
    if (directionChanged == Direction.DOWN) {
      state
    } else if (directionChanged == Direction.UP) {
      this.getUpdatedState(worldIn, state, currentPos)
    } else {
      val redstoneside = this.getSide(worldIn, currentPos, directionChanged)
      if (redstoneside.notNone == state.get(propertyOfDirection(directionChanged)).notNone && !areAllSidesNotNone(state)) {
        state.updated(propertyOfDirection(directionChanged), redstoneside)
      } else {
        this.getUpdatedState(worldIn, this.sideBaseState.updated(propertyOfDirection(directionChanged), redstoneside), currentPos)
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

  override def onBlockAdded(state: BlockState, worldIn: World, pos: BlockPos, oldState: BlockState, isMoving: Boolean): Unit = {
    if (!oldState.isIn(state.getBlock) && !worldIn.isRemote) {
      for (direction <- VERTICAL_DIRECTIONS) {
        worldIn.notifyNeighborsOfStateChange(pos.offset(direction), this)
      }
      this.updateNeighboursStateChange(worldIn, pos)
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

  private def recalculateFacingState(reader: IBlockReader, state: BlockState, pos: BlockPos) = {
    val notNormalCube = !reader.getBlockState(pos.up).isNormalCube(reader, pos)
    HORIZONTAL_DIRECTIONS.foldLeft(state) { (newstate, direction) =>
      val facing = propertyOfDirection(direction)
      if (!newstate.get(facing).notNone) {
        newstate.updated(facing, this.recalculateSide(reader, pos, direction, notNormalCube))
      } else {
        newstate
      }
    }
  }

  private def areAllSidesNotNone(state: BlockState) = {
    state.get(NORTH) != RedstoneSide.NONE &&
      state.get(SOUTH) != RedstoneSide.NONE &&
      state.get(EAST) != RedstoneSide.NONE &&
      state.get(WEST) != RedstoneSide.NONE
  }

  private def getUpdatedState(reader: IBlockReader, oldState: BlockState, pos: BlockPos) = {
    val newstate = this.recalculateFacingState(reader, this.getDefaultState, pos)
    if (areAllSidesNone(oldState) && areAllSidesNone(newstate)) {
      newstate
    } else {
      // This is so if it's only connected on one side, then it continues straight to the opposite side
      val sides = List(NORTH, EAST, SOUTH, WEST)
      val unconnectedSides = sides.map(side => newstate.get(side) == RedstoneSide.NONE)
      sides.indices.foldLeft(newstate) { (state, index) =>
        if(unconnectedSides(index) && unconnectedSides((index + 1) % 4) && unconnectedSides((index + 3) % 4)) {
          state.updated(sides(index), RedstoneSide.SIDE)
        } else {
          state
        }
      }
    }
  }

  private def areAllSidesNone(state: BlockState) = {
    state.get(NORTH) == RedstoneSide.NONE &&
      state.get(SOUTH) == RedstoneSide.NONE &&
      state.get(EAST) == RedstoneSide.NONE &&
      state.get(WEST) == RedstoneSide.NONE
  }

  private def getSide(world: IBlockReader, pos: BlockPos, face: Direction) = {
    this.recalculateSide(world, pos, face, !world.getBlockState(pos.up).isNormalCube(world, pos))
  }

  private def recalculateSide(reader: IBlockReader, pos: BlockPos, direction: Direction, nonNormalCubeAbove: Boolean): RedstoneSide = {
    val blockpos = pos.offset(direction)
    val blockstate = reader.getBlockState(blockpos)
    if (nonNormalCubeAbove &&
      this.canPlaceOnTopOf(reader, blockpos, blockstate) &&
      canConnectTo(reader.getBlockState(blockpos.up), reader, blockpos.up, null)
    ) {
      if (blockstate.isSolidSide(reader, blockpos, direction.getOpposite))
        RedstoneSide.UP
      else
        RedstoneSide.SIDE
    } else {
      if (!canConnectTo(blockstate, reader, blockpos, direction) &&
        (blockstate.isNormalCube(reader, blockpos) || !canConnectTo(reader.getBlockState(blockpos.down), reader, blockpos.down, null))
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
