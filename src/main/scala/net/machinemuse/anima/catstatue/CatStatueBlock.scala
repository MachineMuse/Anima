package net.machinemuse.anima
package catstatue

import bowl.BowlWithContents
import registration.RegistryHelpers.{regBlock, regSimpleBlockItem}
import util.DatagenHelpers.{FancyShapedRecipeBuilder, PartBuilderWorkaround, existingModModelFile, mkLanguageProvider, mkMultipartBlockStates, mkRecipeProvider, mkSimpleBlockItemModel}
import util.VanillaClassEnrichers.RichBlockState

import net.minecraft.block._
import net.minecraft.block.material.{Material, MaterialColor}
import net.minecraft.data.ShapedRecipeBuilder
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item._
import net.minecraft.state.properties.BlockStateProperties
import net.minecraft.state.{DirectionProperty, StateContainer}
import net.minecraft.tileentity.TileEntity
import net.minecraft.util._
import net.minecraft.util.math.shapes.{ISelectionContext, VoxelShape}
import net.minecraft.util.math.{BlockPos, BlockRayTraceResult}
import net.minecraft.world.{IBlockReader, World}
import net.minecraftforge.common.ForgeHooks
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.{FMLConstructModEvent, GatherDataEvent}
import org.apache.logging.log4j.scala.Logging

import scala.annotation.nowarn


/**
 * Created by MachineMuse on 2/11/2021.
 */
object CatStatueBlock extends Logging {
  @SubscribeEvent def onConstructMod(e: FMLConstructModEvent) = {}

  private val properties = AbstractBlock.Properties.create(Material.CLAY, MaterialColor.CLAY)
    .hardnessAndResistance(2.0F).sound(SoundType.BONE).notSolid

  val CAT_STATUE_BLOCK = regBlock("cat_statue", () => new CatStatueBlock(properties))
  val CAT_STATUE_ITEM = regSimpleBlockItem("cat_statue", CAT_STATUE_BLOCK)

  private val HORIZONTAL_DIRECTIONS = List(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)


  val FACING: DirectionProperty = BlockStateProperties.HORIZONTAL_FACING
  val WATERLEVEL = BlockStateProperties.LEVEL_0_3
  val LIT = BlockStateProperties.LIT

  protected val SHAPENORMAL = Block.makeCuboidShape(5.0D, 0.0D, 0.0D, 11.0D, 16.0D, 16.0D)
  protected val SHAPESIDE = Block.makeCuboidShape(5.0D, 0.0D, 5.0D, 16.0D, 16.0D, 11.0D)


  @SubscribeEvent def onGatherData(implicit event: GatherDataEvent): Unit = {
    mkRecipeProvider{ consumer =>
      ShapedRecipeBuilder.shapedRecipe(CAT_STATUE_ITEM.get)
        .patternLine("#  ")
        .patternLine(" ##")
        .patternLine("# #")
        .addKeyAsCriterion('#', Items.CLAY_BALL)
        .setGroup("cat_statue")
        .buildProperly(consumer, "cat_statue")
    }
    mkLanguageProvider("en_us") { lang =>
      // adds as a block i guess because it's an ItemBlock
      lang.addItem(CAT_STATUE_ITEM, "Clay Cat Statue")
    }
    mkLanguageProvider("fr_fr") { lang =>
      lang.addItem(CAT_STATUE_ITEM, "Statue de Chat en Argile")
    }
    mkMultipartBlockStates(CAT_STATUE_BLOCK.get){ builder =>
      val catModel = existingModModelFile("block/cat_statue_base")
      val waterModels = List(existingModModelFile("block/cat_statue_water1"),
        existingModModelFile("block/cat_statue_water2"),
        existingModModelFile("block/cat_statue_water3"))
      for(index <- HORIZONTAL_DIRECTIONS.indices) {
        for(i <- 1 to 3) {
          builder.part.modelFile(waterModels(i-1)).rotationY(index * 90).addModel()
            .saferCondition(FACING, HORIZONTAL_DIRECTIONS(index))
            .saferCondition(WATERLEVEL, i)
        }
        builder.part.modelFile(catModel).rotationY(index * 90).addModel()
          .saferCondition(FACING, HORIZONTAL_DIRECTIONS(index))
      }
    }
    mkSimpleBlockItemModel(CAT_STATUE_BLOCK.get, existingModModelFile("block/cat_statue_base"))

  }

}

@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
class CatStatueBlock(properties: AbstractBlock.Properties) extends Block(properties) with Logging {
  import CatStatueBlock._
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
    logger.info("Cat statue right clicked")
    if(itemInUse.getItem == BowlWithContents.BOWL_OF_WATER.get) {
      val waterLevel = state.get(WATERLEVEL)
      logger.info(s"with bowl of water, level is $waterLevel")
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
        itemInUse.shrink(1)
        player.setHeldItem(hand, itemInUse)
        tileEntity.addFuel(ForgeHooks.getBurnTime(itemInUse))
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
