package net.machinemuse.anima
package util

import com.mojang.datafixers.util
import com.mojang.serialization
import com.mojang.serialization.codecs._
import com.mojang.serialization._
import net.minecraft.entity.ai.attributes.Attribute
import net.minecraft.item.Item
import net.minecraft.util.registry.Registry
import org.apache.logging.log4j.scala.Logging
import shapeless._
import shapeless.ops.hlist.ToTraversable
import shapeless.ops.record.Keys

import java.nio.ByteBuffer
import java.util.stream
import java.util.stream.{IntStream, LongStream, Stream}
import scala.annotation.nowarn
import scala.jdk.CollectionConverters._

/**
 * Created by MachineMuse on 2/6/2021.
 */
object VanillaCodecs extends Logging {
  // Primitives
  implicit val BYTECODEC: Codec[java.lang.Byte] = Codec.BYTE
  implicit val SBYTECODEC: Codec[Byte] = BYTECODEC.xmap (_.byteValue(), new java.lang.Byte(_))

  implicit val BOOLCODEC: Codec[java.lang.Boolean] = Codec.BOOL
  implicit val SBOOLCODEC: Codec[Boolean] = BOOLCODEC.xmap(_.booleanValue(), new java.lang.Boolean(_))

  implicit val DOUBLECODEC: Codec[java.lang.Double] = Codec.DOUBLE
  implicit val SDOUBLECODEC: Codec[Double] = DOUBLECODEC.xmap(_.doubleValue(), new java.lang.Double(_))

  implicit val FLOATCODEC: Codec[java.lang.Float] = Codec.FLOAT
  implicit val SFLOATCODEC: Codec[Float] = FLOATCODEC.xmap(_.floatValue(), new java.lang.Float(_))

  implicit val INTCODEC: Codec[java.lang.Integer] = Codec.INT
  implicit val SINTCODEC: Codec[Int] = INTCODEC.xmap(_.intValue(), new java.lang.Integer(_))

  implicit val LONGCODEC: Codec[java.lang.Long] = Codec.LONG
  implicit val SLONGCODEC: Codec[Long] = LONGCODEC.xmap(_.longValue(), new java.lang.Long(_))

  implicit val SHORTCODEC: Codec[java.lang.Short] = Codec.SHORT
  implicit val SSHORTCODEC: Codec[Short] = SHORTCODEC.xmap(_.shortValue(), new java.lang.Short(_))

  implicit val STRINGCODEC: Codec[String] = Codec.STRING

  // Special
  implicit val PASSTHROUGHCODEC: Codec[serialization.Dynamic[_]] = Codec.PASSTHROUGH
  implicit val INT_STREAMCODEC: Codec[IntStream] = Codec.INT_STREAM
  implicit val BYTE_BUFFERCODEC: Codec[ByteBuffer] = Codec.BYTE_BUFFER
  implicit val LONG_STREAMCODEC: Codec[LongStream] = Codec.LONG_STREAM

  implicit val EMPTYCODEC: MapCodec[util.Unit] = Codec.EMPTY
  implicit val SEMPTYCODEC: MapCodec[Unit] = Codec.EMPTY.xmap(_ => (), _ => util.Unit.INSTANCE)

  // Registries
  implicit val ITEMCODEC: Codec[Item] = Registry.ITEM : @nowarn
  implicit val ATTRIBUTECODEC: Codec[Attribute] = Registry.ATTRIBUTE : @nowarn
  //etc...

  // Generics
  implicit def LISTCODEC[A: Codec]: Codec[java.util.List[A]] = new ListCodec(implicitly[Codec[A]])
  implicit def SLISTCODEC[A: Codec]: Codec[List[A]] = new ListCodec(implicitly[Codec[A]]).xmap (
    _.asScala.toList,
    _.asJava
  )
  //more?

  import JavaFunctionConverters._

  trait HListHasMapCodec[Values <: HList] {
    def genMapCodec(keys: List[String]): MapCodec[Values]
  }

  object HListHasMapCodec {
    type Aux[Values <: HList] = HListHasMapCodec[Values]

    implicit def HNilHasMapCodec = new HListHasMapCodec[HNil] {
      def genMapCodec(keys: List[String]): MapCodec[HNil] = Codec.EMPTY.xmap(_ => HNil, _ => util.Unit.INSTANCE)
    }

    implicit def HSingleHasMapCodec[T <: HList](implicit codec: Codec[T]) = new HListHasMapCodec[T] {
      def genMapCodec(keys: List[String]): MapCodec[T] = codec.fieldOf(keys.head)
    }

    implicit def HConsHasMapCodec[HeadVal, TailVals <: HList]
    (implicit
     tailCodec: HListHasMapCodec.Aux[TailVals],
     headCodec: Codec[HeadVal]
    ) = new HListHasMapCodec[HeadVal :: TailVals] {
      def genMapCodec( keys: List[String]) = {
        val (key, remainder) = (keys.head, keys.tail)
        val mapCodecHead = headCodec.fieldOf(key)
        val mapCodecTail = tailCodec.genMapCodec(remainder)
        val newCodec = MapCodec.of[HeadVal :: TailVals] (
          new MapEncoder.Implementation[HeadVal :: TailVals] {
            override def encode[T](input: HeadVal :: TailVals, ops: DynamicOps[T], prefix: RecordBuilder[T]): RecordBuilder[T] = {
              mapCodecTail.encode(input.tail, ops, mapCodecHead.encode(input.head, ops, prefix))
            }
            override def keys[T](ops: DynamicOps[T]): stream.Stream[T] = Stream.concat(mapCodecHead.keys(ops), mapCodecTail.keys(ops))
          },
          new MapDecoder.Implementation[HeadVal :: TailVals] {
            override def decode[T](ops: DynamicOps[T], input: MapLike[T]): DataResult[HeadVal :: TailVals] = {
              mapCodecTail.decode(ops, input).flatMap(tailVals => mapCodecHead.decode(ops, input).map(headVal => headVal :: tailVals))
            }
            override def keys[T](ops: DynamicOps[T]): stream.Stream[T] = Stream.concat(mapCodecHead.keys(ops), mapCodecTail.keys(ops))
          }
        )
        newCodec
      }
    }

  }

  class CodecMaker[P] {
    def genCaseCodec[Repr <: HList, KeysRepr <: HList, ValuesRepr <: HList]
    (implicit
      lgen: LabelledGeneric.Aux[P, Repr],
      keys: Keys.Aux[Repr, KeysRepr],
      gen: Generic.Aux[P, ValuesRepr],
      traversableSymbol: ToTraversable.Aux[KeysRepr, List, Symbol],
      codecGen: HListHasMapCodec.Aux[ValuesRepr]
    ): Codec[P] = {
      val names = keys().toList.map(_.name)
      codecGen.genMapCodec(names).codec.xmap(repr => gen.from(repr), p => gen.to(p))
    }
  }

  //implicit val ISTRINGSERIALIZABLECODEC: Codec[IStringSerializable] = IStringSerializable.createCodec
  //implicit val Codec
}
