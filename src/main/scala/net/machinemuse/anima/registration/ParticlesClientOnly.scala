package net.machinemuse.anima
package registration

import net.minecraft.client.Minecraft
import net.minecraft.client.particle._
import net.minecraft.client.world.ClientWorld
import net.minecraft.util.math.vector.Vector3d
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent
import net.minecraftforge.eventbus.api.{EventPriority, SubscribeEvent}
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent

import registration.ParticlesGlobal.{AnimaParticleData, MOTE}
import util.{Colour, Logging}


/**
 * Created by MachineMuse on 2/5/2021.
 */
@EventBusSubscriber(modid = Anima.MODID, value = Array(Dist.CLIENT), bus = Bus.MOD)
object ParticlesClientOnly extends Logging {

  @SubscribeEvent def onClientSetup(event: FMLClientSetupEvent): Unit = { }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  def registerParticles(event: ParticleFactoryRegisterEvent): Unit = {
    Minecraft.getInstance().particles.registerFactory[AnimaParticleData](MOTE.get, new MoteFactory(_))
  }

  class Mote(world: ClientWorld, pos: Vector3d, m: Vector3d, data: AnimaParticleData) extends SpriteTexturedParticle(world, pos.x, pos.y, pos.z, m.x, m.y, m.z) {
    (this.setColor _).tupled(Colour.toTuple(data.colour))

    this.setSize(data.size.toFloat, data.size.toFloat)
    this.particleScale = data.size.toFloat
    // particle default constructor randomly perturbs input motion so let's stop that
    this.motionX = m.x
    this.motionY = m.y
    this.motionZ = m.z

    this.maxAge = data.lifeticks
    this.particleGravity = data.gravity

    this.canCollide = data.doCollision

    this.particleAngle

    override def tick(): Unit = {
      if(data.spin != 0){
        this.prevParticleAngle = this.particleAngle
        this.particleAngle += data.spin
        if (this.onGround) {
          this.prevParticleAngle = 0.0F
          this.particleAngle = 0.0F
        }
      }
      super.tick()
    }
    override val getRenderType: IParticleRenderType = IParticleRenderType.PARTICLE_SHEET_TRANSLUCENT
  }

  class MoteFactory(sprite: IAnimatedSprite) extends IParticleFactory[AnimaParticleData] {
    val spriteSet = sprite

    override def makeParticle(data: AnimaParticleData, world: ClientWorld, x: Double, y: Double, z: Double, xSpeed: Double, ySpeed: Double, zSpeed: Double): Particle = {
      val mote = new Mote(world, new Vector3d(x,y,z), new Vector3d(xSpeed, ySpeed, zSpeed), data)
      mote.selectSpriteWithAge(spriteSet)
      mote
    }
  }

}
