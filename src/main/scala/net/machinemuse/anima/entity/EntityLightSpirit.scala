package net.machinemuse.anima
package entity

import entity.EntityLightSpirit.{AirLightBlock, getAirLightBlock}
import registration.ParticlesGlobal.AnimaParticleData
import registration.RegistryHelpers._
import util.BlockStateFlags
import util.RichDataParameter._

import net.minecraft.block._
import net.minecraft.block.material.Material
import net.minecraft.entity._
import net.minecraft.item.DyeColor
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.shapes._
import net.minecraft.world.{IBlockReader, World}
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent
import org.apache.logging.log4j.scala.Logging

import scala.util.Random

/**
 * Created by MachineMuse on 1/25/2021.
 */
object EntityLightSpirit extends ParameterRegistrar(classOf[EntityLightSpirit]) with Logging {
  @SubscribeEvent def onConstructMod(event: FMLConstructModEvent) = {}

  private val ENTITY_LIGHT_SPIRIT = regEntityType[EntityLightSpirit]("lightspirit", () => EntityLightSpirit, new EntityLightSpirit(_,_), EntityClassification.MISC)
  def getType = ENTITY_LIGHT_SPIRIT.get()

  private val AIRLIGHT_BLOCK = regBlock("airlight", () => new AirLightBlock)
  def getAirLightBlock = AIRLIGHT_BLOCK.get()

  class AirLightBlock extends Block(AbstractBlock.Properties.create(Material.AIR).doesNotBlockMovement.noDrops.setLightLevel(_ => 15)) {
    override def getRenderType(state: BlockState): BlockRenderType = BlockRenderType.INVISIBLE
    override def getShape(state: BlockState, worldIn: IBlockReader, pos: BlockPos, context: ISelectionContext): VoxelShape = VoxelShapes.empty
  }

}

@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
class EntityLightSpirit (entityType: EntityType[EntityLightSpirit], world: World) extends Entity(entityType, world) with DataHandlingEntity with Logging {
  override def registrar = EntityLightSpirit
  val homeblock: DataSync[BlockPos] = mkDataSync("homeblock", new BlockPos(0, 62, 0))
  val attention: DataSync[Int] =      mkDataSync("attention", 1000)

  private val colour = Random.nextInt(DyeColor.WHITE.getTextColor)

  private var lasttryticks: Int = 20

  override def tick(): Unit = {
    world.onServer{ serverWorld =>
      attention -= 1
      val homeBlock = homeblock.get()
      if(Random.nextInt(100) == 0) {
        serverWorld.spawnParticle(AnimaParticleData(colour = colour), homeBlock.getX + .5, homeBlock.getY + .5, homeBlock.getZ + .5, 1, 0.0D, 0.0D, 0.0D, 0.0)
      }
      if(attention.get <= 0){
        val block = serverWorld.getBlockState(homeBlock)
        if(block.getBlock.isInstanceOf[AirLightBlock]) {
          serverWorld.setBlockState(homeBlock, Blocks.AIR.getDefaultState, BlockStateFlags.STANDARD_CLIENT_UPDATE)
        }
        this.remove(false)
      } else {
        if(serverWorld.isAirBlock(homeBlock)) {
          val block = serverWorld.getBlockState(homeBlock)
          if(!block.getBlock.isInstanceOf[AirLightBlock]) {
            lasttryticks -= 1
            if(lasttryticks < 0) {
              logger.debug("spirit tick: Block at " + homeBlock + " : " + block.getBlock)
              serverWorld.setBlockState(homeBlock, getAirLightBlock.getDefaultState, BlockStateFlags.STANDARD_CLIENT_UPDATE)
              lasttryticks = 20
            }
          }
        }
      }
    }
    super.tick()
  }


}
