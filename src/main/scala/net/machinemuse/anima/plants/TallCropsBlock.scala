package net.machinemuse.anima
package plants

import net.minecraft.block.Block.spawnDrops
import net.minecraft.block.CropsBlock.getGrowthChance
import net.minecraft.block._
import net.minecraft.block.material.Material
import net.minecraft.client.renderer.{RenderType, RenderTypeLookup}
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.state._
import net.minecraft.state.properties.BlockStateProperties
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.shapes.VoxelShape
import net.minecraft.world.World
import net.minecraft.world.server.ServerWorld
import net.minecraftforge.api.distmarker.{Dist, OnlyIn}
import net.minecraftforge.common.{ForgeHooks, IPlantable}
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.{FMLClientSetupEvent, FMLConstructModEvent}

import java.util.Random
import java.util.function.Supplier
import scala.annotation.nowarn
import scala.jdk.CollectionConverters.CollectionHasAsScala

import constants.BlockStateFlags.{STANDARD_CLIENT_UPDATE, STANDARD_MULTIBLOCK_BREAK_UPDATE}
import registration.RegistryHelpers._
import util.DatagenHelpers.mkCenteredCuboidShape
import util.Logging
import util.VanillaClassEnrichers.RichBlockState

/**
 * Created by MachineMuse on 2/15/2021.
 */
@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
object TallCropsBlock extends Logging {
  @SubscribeEvent def onConstructMod(event: FMLConstructModEvent) = {}

  @OnlyIn(Dist.CLIENT) @SubscribeEvent def onClientSetup(event: FMLClientSetupEvent) = {
    RenderTypeLookup.setRenderLayer(WOAD_FLOWERS_BLOCK.get, RenderType.getCutout)
    RenderTypeLookup.setRenderLayer(WOAD_LEAVES_BLOCK.get, RenderType.getCutout)
  }

  val AGE: IntegerProperty = BlockStateProperties.AGE_0_7
  val HAS_FLOWERS: BooleanProperty = BooleanProperty.create("has_flowers")

  protected val SPROUT_SHAPE: VoxelShape = mkCenteredCuboidShape(4.0, 3.0)
  protected val TRIMMED_SHAPE: VoxelShape = mkCenteredCuboidShape(16.0, 10.0)
  protected val FULL_SHAPE: VoxelShape = mkCenteredCuboidShape(16.0, 16.0)
  protected val FLOWERS_SHAPE: VoxelShape = mkCenteredCuboidShape(16.0, 10.0)

  protected val leaves_properties = AbstractBlock.Properties.create(Material.PLANTS)
    .doesNotBlockMovement
    .tickRandomly
    .zeroHardnessAndResistance
    .sound(SoundType.PLANT)

  protected val flowers_properties = AbstractBlock.Properties.create(Material.PLANTS)
    .doesNotBlockMovement
    .zeroHardnessAndResistance
    .sound(SoundType.PLANT)

  val WOAD_FLOWERS_BLOCK = regBlock("woad_flowers", () => new TallCropsHeadBlock(flowers_properties))
  val WOAD_LEAVES_BLOCK = regBlock("woad_leaves", () => new TallCropsBodyBlock(WOAD_FLOWERS_BLOCK, AGE, leaves_properties))

  val WOAD_LEAVES_ITEM = regSimpleItem("woad_leaves")
  val WOAD_SEEDS = regNamedBlockItem("woad_seeds", WOAD_LEAVES_BLOCK)

  class TallCropsHeadBlock(properties: AbstractBlock.Properties) extends Block(properties) {
    override def onBlockHarvested(world : World, pos : BlockPos, state : BlockState, player : PlayerEntity): Unit = {
      if(!world.isRemote) {
        if(!player.isCreative) {
          val down = pos.down()
          val downstate = world.getBlockState(down)
          spawnDrops(downstate, world, down, null, player, player.getHeldItemMainhand)
        }
        removeBottomHalf(world, pos, state, player)
      }
      super.onBlockHarvested(world, pos, state, player)
    }

    protected def removeBottomHalf(world: World, pos: BlockPos, state: BlockState, player: PlayerEntity): Unit = {
      val downpos = pos.down
      val downstate = world.getBlockState(downpos)
      if (downstate.getBlock.isInstanceOf[TallCropsBodyBlock]) {
        world.setBlockState(downpos, Blocks.AIR.getDefaultState, STANDARD_MULTIBLOCK_BREAK_UPDATE)
        world.playEvent(player, 2001, downpos, Block.getStateId(downstate))
      }
    }
  }

  class TallCropsBodyBlock(head: Supplier[TallCropsHeadBlock], age: IntegerProperty, properties: AbstractBlock.Properties) extends CropsBlock(properties) with IGrowable with IPlantable {
    setDefaultState(this.stateContainer.getBaseState.updated(HAS_FLOWERS, false))

    override def fillStateContainer(container : StateContainer.Builder[Block, BlockState]): Unit = {
      container.add(HAS_FLOWERS)
      super.fillStateContainer(container)
    }

    override def randomTick(state : BlockState, world : ServerWorld, pos : BlockPos, random : Random): Unit = {
      val chance = random.nextDouble() * randomTickMultiplier % 8.0
      for(_ <- 0 to extraRandomTicks) {
        super.randomTick(state, world, pos, random)
      }
      if(chance < 1) {
        super.randomTick(state, world, pos, random)
      }
      if(state.get(age) >= getMaxAge && world.getBlockState(pos.up()).isAir()){
        val f = getGrowthChance(this, world, pos)
        if(ForgeHooks.onCropsGrowPre(world, pos, state, random.nextInt((25.0f / f + 1).toInt) == 0)) {
          world.setBlockState(pos.up, head.get.getDefaultState, STANDARD_CLIENT_UPDATE)
          world.setBlockState(pos, state.updated(HAS_FLOWERS, true))
          ForgeHooks.onCropsGrowPost(world, pos, state)
        }
      }
    } : @nowarn

    override def isMaxAge(state : BlockState): Boolean = {
      state.get(this.getAgeProperty) >= this.getMaxAge && state.get(HAS_FLOWERS)
    }

    override def onBlockHarvested(world : World, pos : BlockPos, state : BlockState, player : PlayerEntity): Unit = {
      if(!world.isRemote) {
        removeTopHalf(world, pos, state, player)
        if(!player.isCreative) {
          spawnDrops(state, world, pos, null, player, player.getHeldItemMainhand)
        }
      }
      super.onBlockHarvested(world, pos, state, player)
    }

    protected def removeTopHalf(world: World, pos: BlockPos, state: BlockState, player: PlayerEntity): Unit = {
      val uppos = pos.up
      val upstate = world.getBlockState(uppos)
      if (upstate.getBlock == head.get) {
        world.setBlockState(uppos, Blocks.AIR.getDefaultState, STANDARD_MULTIBLOCK_BREAK_UPDATE)
        world.playEvent(player, 2001, uppos, Block.getStateId(upstate))
      }
    }
    override val getMaxAge = age.getAllowedValues.asScala.map(_.intValue()).max
    private val randomTickMultiplier = 8.0 / (getMaxAge.toDouble % 8.0)
    private val extraRandomTicks = (getMaxAge.toDouble / 8).toInt
  }
}

