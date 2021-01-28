package net.machinemuse.anima.entity

import net.machinemuse.anima.entity.EntityLightSpirit.{LOGGER, params}
import net.machinemuse.anima.registration.AnimaRegistry
import net.machinemuse.anima.util.BlockStateFlags
import net.machinemuse.anima.util.RichDataParameter._
import net.minecraft.block.Blocks
import net.minecraft.entity.{Entity, EntityType}
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import org.apache.logging.log4j.LogManager
import shapeless.syntax.std.tuple.productTupleOps

/**
 * Created by MachineMuse on 1/25/2021.
 */
object EntityLightSpirit extends ParameterRegistrar(classOf[EntityLightSpirit]){
  private val LOGGER = LogManager.getLogger

  val params = (
    mkDataParameter("homeblock", new BlockPos(0, 62, 0)), // which block it will attempt to light up
    mkDataParameter("attention", (1000)), // how long before it departs this realm (in ticks)
    mkDataParameter("last_try", (20)) // how often to try to update the block (ticks)
  )

}
class EntityLightSpirit (entityType: EntityType[EntityLightSpirit], world: World) extends Entity(entityType, world) with DataHandlingEntity {
  override def dataParameters: List[ParameterInstance[_]] = params.toList
  val (homeblock: Access[BlockPos], attention: Access[Int], lasttryticks: Access[Int]) = params.zipConst(dataManager).map(Par2Access)

  override def tick(): Unit = {
    attention.set(attention.get - 1)
    val homeBlock = homeblock.get()
    if(attention.get <= 0){
      val block = world.getBlockState(homeBlock)
      if(block.getBlock.isInstanceOf[AirLightBlock]) {
        world.setBlockState(homeBlock, Blocks.AIR.getDefaultState, BlockStateFlags.STANDARD_CLIENT_UPDATE)
      }
      this.remove(false)
    } else {
      if(world.isAirBlock(homeBlock)) {
        if(!world.isRemote) {
          val block = world.getBlockState(homeBlock)
          if(!block.getBlock.isInstanceOf[AirLightBlock]) {
            lasttryticks.set(lasttryticks.get - 1)
            if(lasttryticks.get < 0) {
              LOGGER.info("spirit tick: Block at " + homeBlock + " : " + block.getBlock)

              world.setBlockState(homeBlock, AnimaRegistry.AIRLIGHT_BLOCK.get.getDefaultState, BlockStateFlags.STANDARD_CLIENT_UPDATE)
              lasttryticks.set(20)
            }
          }
        }
      }
    }
    super.tick()
  }


}
