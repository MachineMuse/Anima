package net.machinemuse.anima
package registration

import com.mojang.serialization.Codec
import net.minecraft.item.DyeColor
import net.minecraft.network.PacketBuffer
import net.minecraft.particles.{IParticleData, ParticleType}
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.RegistryObject
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent
import org.apache.logging.log4j.scala.Logging

import registration.RegistryHelpers.PARTICLES
import util.GenCodecsByName._
import util.VanillaCodecs._

/**
 * Created by MachineMuse on 2/5/2021.
 */

@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
object ParticlesGlobal extends Logging {
  @SubscribeEvent def onConstructMod(e: FMLConstructModEvent) = {}

  /*_*/ // Disable type-aware highlighting for this val 'cause IDEA can't find all the implicits
  val CODEC: Codec[AnimaParticleData] = implicitly[Codec[AnimaParticleData]]

  val MOTE: RegistryObject[ParticleType[AnimaParticleData]] = PARTICLES.register("mote", () => CODEC.mkParticleType(true))
  /*_*/

  case class AnimaParticleData( size: Double = 1.0,
                                colour: Int = DyeColor.WHITE.getTextColor,
                                lifeticks: Int = 5.secondsInTicks,
                                doCollision: Boolean = true,
                                gravity: Float = 0.0f,
                                spin: Float = 0.0f) extends IParticleData with CodecByName {

    override def getType: ParticleType[_] = MOTE.get()

    override def write(buffer: PacketBuffer): Unit = CODEC.writeCompressed(buffer, this)

    override def getParameters: String = {
      val names = this.productElementNames.toList
      val values = this.productIterator.toList
      val elems = names.zip(values).collect{case (name, value) => s"$name = $value"}
      s"Parameters: [${elems.mkString(", ")}]"
    }
  }
}
