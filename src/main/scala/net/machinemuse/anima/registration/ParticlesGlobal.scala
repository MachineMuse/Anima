package net.machinemuse.anima
package registration

import registration.RegistryHelpers.PARTICLES
import util.VanillaCodecs._

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.serialization.Codec
import net.minecraft.item.DyeColor
import net.minecraft.network.PacketBuffer
import net.minecraft.particles.{IParticleData, ParticleType}
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.RegistryObject
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent
import org.apache.logging.log4j.scala.Logging

import scala.annotation.nowarn

/**
 * Created by MachineMuse on 2/5/2021.
 */

@Mod.EventBusSubscriber(modid = Anima.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
object ParticlesGlobal extends Logging {
  @SubscribeEvent def onConstructMod(e: FMLConstructModEvent) = {}

  val MOTE: RegistryObject[ParticleType[AnimaParticleData]] = PARTICLES.register("mote", () => new AnimaParticleType)

  @nowarn
  object AnimaParticleDeserializer extends IParticleData.IDeserializer[AnimaParticleData]  {
    @throws[CommandSyntaxException]
    override def deserialize(pt: ParticleType[AnimaParticleData], reader: StringReader): AnimaParticleData = {
      AnimaParticleData(size = reader.readDouble, colour = reader.readInt, lifeticks = reader.readInt, doCollision = reader.readBoolean, gravity = reader.readFloat, spin = reader.readFloat)
      //TODO: command spawning particles?
    }

    override def read(pt: ParticleType[AnimaParticleData], buffer: PacketBuffer): AnimaParticleData = {
      AnimaParticleData(size = buffer.readDouble, colour = buffer.readInt, lifeticks = buffer.readInt, doCollision = buffer.readBoolean, gravity = buffer.readFloat, spin = buffer.readFloat)
    }
  }
  /*_*/ // Disable type-aware highlighting for this val 'cause IDEA can't find all the implicits
  import HListHasMapCodec._
  val CODEC: Codec[AnimaParticleData] = new CodecMaker[AnimaParticleData].genCaseCodec
  /*_*/

  class AnimaParticleType extends ParticleType[AnimaParticleData](true, AnimaParticleDeserializer) {
    override def func_230522_e_(): Codec[AnimaParticleData] = CODEC
  }

  case class AnimaParticleData( size: Double = 1.0,
                                colour: Int = DyeColor.WHITE.getTextColor,
                                lifeticks: Int = 5.secondsInTicks,
                                doCollision: Boolean = true,
                                gravity: Float = 0.0f,
                                spin: Float = 0.0f) extends IParticleData {

    override def getType: ParticleType[_] = MOTE.get()

    override def write(buffer: PacketBuffer): Unit = {
      buffer.writeDouble(size)
      buffer.writeInt(colour)
      buffer.writeInt(lifeticks)
      buffer.writeBoolean(doCollision)
      buffer.writeFloat(gravity)
      buffer.writeFloat(spin)
    }

    override def getParameters: String = {
      val names = this.productElementNames.toList
      val values = this.productIterator.toList
      val elems = names.zip(values).collect{case (name, value) => s"$name = $value"}
      s"Parameters: [${elems.mkString(", ")}]"
    }
  }
}
