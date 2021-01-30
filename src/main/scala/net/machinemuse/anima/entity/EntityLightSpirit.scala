package net.machinemuse.anima
package entity

import net.machinemuse.anima.registration.AnimaRegistry
import net.machinemuse.anima.util.BlockStateFlags
import net.machinemuse.anima.util.RichDataParameter._
import net.minecraft.block.Blocks
import net.minecraft.entity.{Entity, EntityType}
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import org.apache.logging.log4j.scala.Logging

/**
 * Created by MachineMuse on 1/25/2021.
 */
object EntityLightSpirit extends ParameterRegistrar(classOf[EntityLightSpirit]) with Logging {

}
class EntityLightSpirit (entityType: EntityType[EntityLightSpirit], world: World) extends Entity(entityType, world) with DataHandlingEntity with Logging {
  override def registrar = EntityLightSpirit
  val homeblock: Access[BlockPos] = mkData("homeblock", new BlockPos(0, 62, 0))
  val attention: Access[Int] = mkData("attention", 1000)
  private var lasttryticks: Int = 20

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
            lasttryticks = lasttryticks - 1
            if(lasttryticks < 0) {
              logger.debug("spirit tick: Block at " + homeBlock + " : " + block.getBlock)

              world.setBlockState(homeBlock, AnimaRegistry.AIRLIGHT_BLOCK.get.getDefaultState, BlockStateFlags.STANDARD_CLIENT_UPDATE)
              lasttryticks = 20
            }
          }
        }
      }
    }
    super.tick()
  }


}
