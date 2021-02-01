package net.machinemuse.anima

import net.minecraft.network.PacketBuffer
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.network.{NetworkEvent, NetworkRegistry}
import org.apache.logging.log4j.scala.Logging

import java.util.function.{BiConsumer, Supplier}
import scala.reflect.{ClassTag, classTag}

/**
 * Created by MachineMuse on 2/1/2021.
 */
object Network extends Logging {
  // TODO: Better protocol version handling
  private val PROTOCOL_VERSION = "1"
  val CHANNEL = NetworkRegistry.newSimpleChannel(
    new ResourceLocation("mymodid", "main"),
    () => PROTOCOL_VERSION,
    PROTOCOL_VERSION.equals,
    PROTOCOL_VERSION.equals
  )
  private var NETWORK_ID: Int = 0

  def registerMessage[P: ClassTag](
                       encode: (P, PacketBuffer) => (),
                       decode: PacketBuffer => P,
                       handle: (P, () => NetworkEvent.Context) => ()) = {
    val clazz: Class[P] = classTag[P].runtimeClass.asInstanceOf[Class[P]]
    import JavaFunctionConverters._
    CHANNEL.registerMessage(NETWORK_ID, clazz, encode, decode, handle)
    NETWORK_ID = NETWORK_ID + 1
  }

  def registerCaseMessage[P <: CasePacket : ClassTag](c: CaseNetCodec[P]) = {
    val clazz: Class[P] = classTag[P].runtimeClass.asInstanceOf[Class[P]]
    val handler: BiConsumer[P, Supplier[NetworkEvent.Context]] = (p, s) => {
      val ctx = s.get()
      ctx.enqueueWork { () =>
        p.handle(ctx)
      }
      ctx.setPacketHandled(true)
    }
    CHANNEL.registerMessage(NETWORK_ID, clazz, c.mkEncode, c.mkDecode, handler)
    NETWORK_ID = NETWORK_ID + 1
  }

  trait NetCodec[A] {
    def encode(buf: PacketBuffer, a: A): Unit
    def decode(buf: PacketBuffer): A
  }
  private def mkNetCodec[A](enc: (PacketBuffer, A) => Unit, dec: PacketBuffer => A) = {
    new NetCodec[A] {
      override def encode(buf: PacketBuffer, a: A): Unit = enc(buf, a)
      override def decode(buf: PacketBuffer): A = dec(buf)
    }
  }

  implicit val IntNetCodec = mkNetCodec[Int] (_.writeInt(_), _.readInt())
  // TODO: other serializable types :D
  // TODO: More arities
  // Probably do these ^ as needed

  trait CasePacket {
    def handle(context: NetworkEvent.Context): Unit
  }

  trait CaseNetCodec[P] {
    def mkEncode: (P, PacketBuffer) => Unit
    def mkDecode: PacketBuffer => P
  }

  case class CaseNetCodec1[A: NetCodec, P](ap: A => P, unap: P => Option[A]) extends CaseNetCodec[P] {
    override def mkEncode: (P, PacketBuffer) => Unit =
      (p, buf: PacketBuffer) => unap(p) match {
        case Some(a) =>
          implicitly[NetCodec[A]].encode(buf, a)
        case None => logger.error("Tried to encode a null packet: " + p)
      }

    override def mkDecode: PacketBuffer => P =
      (buf: PacketBuffer) => ap(
        implicitly[NetCodec[A]].decode(buf)
      )
  }

  case class CaseNetCodec2[A: NetCodec, B: NetCodec, P](ap: (A, B) => P, unap: P => Option[(A, B)]) extends CaseNetCodec[P] {
    override def mkEncode: (P, PacketBuffer) => Unit =
      (p, buf: PacketBuffer) => unap(p) match {
        case Some((a, b)) =>
          implicitly[NetCodec[A]].encode(buf, a)
          implicitly[NetCodec[B]].encode(buf, b)
        case None => logger.error("Tried to encode a null packet: " + p)
      }

    override def mkDecode: PacketBuffer => P =
      (buf: PacketBuffer) => ap(
        implicitly[NetCodec[A]].decode(buf),
        implicitly[NetCodec[B]].decode(buf)
      )
  }

  case class CaseNetCodec3[A: NetCodec, B: NetCodec, C: NetCodec, P](ap: (A, B, C) => P, unap: P => Option[(A, B, C)]) extends CaseNetCodec[P] {
    override def mkEncode: (P, PacketBuffer) => Unit =
      (p, buf: PacketBuffer) => unap(p) match {
        case Some((a, b, c)) =>
          implicitly[NetCodec[A]].encode(buf, a)
          implicitly[NetCodec[B]].encode(buf, b)
          implicitly[NetCodec[C]].encode(buf, c)
        case None => logger.error("Tried to encode a null packet: " + p)
      }

    override def mkDecode: PacketBuffer => P =
      (buf: PacketBuffer) => ap(
        implicitly[NetCodec[A]].decode(buf),
        implicitly[NetCodec[B]].decode(buf),
        implicitly[NetCodec[C]].decode(buf)
      )
  }
//
//
//  case class CasePacket2[A: NetCodec, B: NetCodec](a: A, b: B) extends CasePacket {
//    override def encode(buf: PacketBuffer) = {
//      implicitly[NetCodec[A]].encode(buf, a)
//      implicitly[NetCodec[B]].encode(buf, b)
//    }
//
//  }
//  def registerMessageC1[A: NetCodec](handle: (A, NetworkEvent.Context) => ()) = {
//    registerMessage[CasePacket1[A]](
//      (p, buf) => implicitly[NetCodec[A]].encode(buf, p.a),
//      buf => CasePacket1(implicitly[NetCodec[A]].decode(buf)),
//      (p: CasePacket1[A], ctxs) => handle(p.a, ctxs())
//    )
//  }
}
