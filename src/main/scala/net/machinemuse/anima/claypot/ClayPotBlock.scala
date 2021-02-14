package net.machinemuse.anima
package claypot

import net.minecraft.block._
import net.minecraft.block.material.{Material, MaterialColor}
import net.minecraft.data.ShapedRecipeBuilder
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.{BlockItemUseContext, Items}
import net.minecraft.state._
import net.minecraft.state.properties.BlockStateProperties
import net.minecraft.util.math.shapes.{ISelectionContext, VoxelShape}
import net.minecraft.util.math.{BlockPos, BlockRayTraceResult}
import net.minecraft.util.{Unit => _, _}
import net.minecraft.world.{IBlockReader, World}
import net.minecraftforge.client.model.generators.ConfiguredModel
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.{FMLConstructModEvent, GatherDataEvent}
import org.apache.logging.log4j.scala.Logging

import scala.annotation.nowarn

import constants.BlockStateFlags
import registration.RegistryHelpers.{regBlock, regSimpleBlockItem}
import util.DatagenHelpers.{FancyShapedRecipeBuilder, existingModModelFile, mkAllVariantBlockStates, mkLanguageProvider, mkRecipeProvider, mkSimpleBlockItemModel}

/**
 * Created by MachineMuse on 2/9/2021.
 */
object ClayPotBlock extends Logging {

  @SubscribeEvent def onConstructMod(e: FMLConstructModEvent) = {}
  private val properties = AbstractBlock.Properties.create(Material.CLAY, MaterialColor.CLAY)
    .hardnessAndResistance(2.0F).sound(SoundType.BONE).notSolid
  val BLOCK = regBlock("clay_pot", () => new ClayPotBlock(properties))
  val ITEM = regSimpleBlockItem("clay_pot", BLOCK)

  private val OPEN: BooleanProperty = BlockStateProperties.OPEN
  private val FACING: DirectionProperty = BlockStateProperties.HORIZONTAL_FACING

  protected val SHAPE = Block.makeCuboidShape(1.0D, 0.0D, 1.0D, 15.0D, 12.0D, 15.0D)

  @SubscribeEvent def onGatherData(implicit event: GatherDataEvent): Unit = {
    mkRecipeProvider{ consumer =>
          ShapedRecipeBuilder.shapedRecipe(ITEM.get)
            .patternLine(" / ")
            .patternLine("# #")
            .patternLine("###")
            .addKeyAsCriterion('/', Items.STICK)
            .addKeyAsCriterion('#', Items.CLAY_BALL)
            .setGroup("clay_pot")
            .buildProperly(consumer, "clay_pot")
        }
    mkLanguageProvider("en_us"){ lang =>
      // adds as a block i guess because it's an ItemBlock
      lang.addItem(ITEM, "Clay Pot")
    }
    mkLanguageProvider("fr_fr"){ lang =>
      lang.addItem(ITEM, "Pot en Argile")
    }
    mkAllVariantBlockStates(BLOCK.get){ state =>
      if(state.get(OPEN)) {
        ConfiguredModel.builder()
          .modelFile(existingModModelFile("block/clay_pot_open"))
          .rotationY(state.get(FACING).getHorizontalIndex * 90)
          .build()
      } else {
        ConfiguredModel.builder()
          .modelFile(existingModModelFile("block/clay_pot"))
          .rotationY(state.get(FACING).getHorizontalIndex * 90)
          .build()
      }
    }
    mkSimpleBlockItemModel(BLOCK.get, existingModModelFile("block/clay_pot"))

  }
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
