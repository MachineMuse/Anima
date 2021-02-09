package net.machinemuse.anima
package util

import com.mojang.datafixers.util
import util.VanillaCodecs.{EMPTYCODEC, STRINGCODEC}

import com.mojang.serialization._
import org.apache.logging.log4j.scala.Logging
import shapeless.{:+:, ::, CNil, Coproduct, HList, HNil, Inl, Inr}

import java.util.stream.Stream
import java.util.{Optional, stream}
import scala.jdk.OptionConverters.{RichOption, RichOptional}

/**
 * Created by MachineMuse on 2/8/2021.
 */
// For when we want to explicitly tag stuff
object GenCodecsWithKeys extends Logging {
  private final val TYPENAMECODEC = STRINGCODEC.fieldOf("type")
  trait HasMapCodecWithKeys[Values] {
    def genMapCodec(keys: List[String]): MapCodec[Values]
  }
  object HasMapCodecWithKeys {
    type Aux[Values] = HasMapCodecWithKeys[Values]
    sealed trait KeyOptionalizer[V] {
      def genField(key: String): MapCodec[V]
    }
    implicit def OptionalOptionalizer[V](implicit codec: Codec[V]): KeyOptionalizer[Optional[V]] = new KeyOptionalizer[Optional[V]] {
      override def genField(key: String): MapCodec[Optional[V]] = codec.optionalFieldOf(key)
    }
    implicit def OptionOptionalizer[V](implicit codec: Codec[V]): KeyOptionalizer[Option[V]] = new KeyOptionalizer[Option[V]] {
      override def genField(key: String): MapCodec[Option[V]] = codec.optionalFieldOf(key).xmap(_.toScala, _.toJava)
    }
    implicit def NonOptionOptionalizer[V](implicit codec: Codec[V]): KeyOptionalizer[V] = new KeyOptionalizer[V] {
      override def genField(key: String): MapCodec[V] = codec.fieldOf(key)
    }
    implicit val HNilHasMapCodecWithKeys: HasMapCodecWithKeys[HNil] = (keys: List[String]) => Codec.EMPTY.xmap(_ => HNil, _ => util.Unit.INSTANCE)

    implicit def HConsHasMapCodecWithKeys[Head, TailVals <: HList]
    (implicit
     headOptionalizer: KeyOptionalizer[Head],
     tailCodec: HasMapCodecWithKeys.Aux[TailVals]
    ): HasMapCodecWithKeys[Head :: TailVals] = (keys: List[String]) => {
      val (key, remainder) = (keys.head, keys.tail)
      val mapCodecHead = headOptionalizer.genField(key)
      val mapCodecTail = tailCodec.genMapCodec(remainder)
      MapCodec.of[Head :: TailVals] (
        new MapEncoder.Implementation[Head :: TailVals] {
          override def encode[T](input: Head :: TailVals, ops: DynamicOps[T], prefix: RecordBuilder[T]): RecordBuilder[T] =
            mapCodecTail.encode(input.tail, ops, mapCodecHead.encode(input.head, ops, prefix))
          override def keys[T](ops: DynamicOps[T]): stream.Stream[T] =
            Stream.concat(mapCodecHead.keys(ops), mapCodecTail.keys(ops))
        },
        new MapDecoder.Implementation[Head :: TailVals] {
          override def decode[T](ops: DynamicOps[T], input: MapLike[T]): DataResult[Head :: TailVals] =
            mapCodecTail.decode(ops, input).flatMap(tailVals => mapCodecHead.decode(ops, input).map(headVal => headVal :: tailVals))
          override def keys[T](ops: DynamicOps[T]): stream.Stream[T] =
            Stream.concat(mapCodecHead.keys(ops), mapCodecTail.keys(ops))
        }
      )
    }

    implicit def CNilHasMapCodecWithKeys: HasMapCodecWithKeys[CNil] = (keys: List[String]) => MapCodec.of[CNil](
      new MapEncoder.Implementation[CNil] {
        override def encode[T](input: CNil, ops: DynamicOps[T], prefix: RecordBuilder[T]): RecordBuilder[T] = EMPTYCODEC.encode(util.Unit.INSTANCE, ops, prefix)
        override def keys[T](ops: DynamicOps[T]): Stream[T] = Stream.empty()
      },
      new MapDecoder.Implementation[CNil] {
        override def decode[T](ops: DynamicOps[T], input: MapLike[T]): DataResult[CNil] = DataResult.error("Invalid type tag for sum type: " + input)
        override def keys[T](ops: DynamicOps[T]): Stream[T] = Stream.empty()
      }
    )

    implicit def CConsHasMapCodecWithKeys[Head, Tail <: Coproduct]
    (implicit
     mapCodecHead: MapCodec[Head],
     tailCodec: HasMapCodecWithKeys.Aux[Tail]
    ): HasMapCodecWithKeys[Head :+: Tail] = (keys: List[String]) => {
      val (key, remainder) = (keys.head, keys.tail)
      val mapCodecTail = tailCodec.genMapCodec(remainder)
      MapCodec.of[Head :+: Tail](
        new MapEncoder.Implementation[Head :+: Tail] {
          override def encode[T](input: Head :+: Tail, ops: DynamicOps[T], prefix: RecordBuilder[T]): RecordBuilder[T] =
            input match {
              case Inl(h) => TYPENAMECODEC.encode(key, ops, mapCodecHead.encode(h, ops, prefix))
              case Inr(t) => mapCodecTail.encode(t, ops, prefix)
            }
          override def keys[T](ops: DynamicOps[T]): Stream[T] = Stream.concat(mapCodecHead.keys(ops), mapCodecTail.keys(ops))
        },
        new MapDecoder.Implementation[Head :+: Tail] {
          override def decode[T](ops: DynamicOps[T], input: MapLike[T]): DataResult[Head :+: Tail] =
            TYPENAMECODEC.decode(ops, input).flatMap { typ =>
              if (typ == key)
                mapCodecHead.decode(ops, input).map(curr => Inl(curr))
              else
                mapCodecTail.decode(ops, input).map(tail => Inr(tail))
            }
          override def keys[T](ops: DynamicOps[T]): Stream[T] = Stream.concat(mapCodecHead.keys(ops), mapCodecTail.keys(ops))
        }
      )
    }
  }

}
