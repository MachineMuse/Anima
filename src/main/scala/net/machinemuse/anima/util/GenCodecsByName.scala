package net.machinemuse.anima
package util

import com.mojang.datafixers.util
import com.mojang.serialization._
import org.apache.logging.log4j.scala.Logging
import shapeless.labelled.{FieldType, field}
import shapeless.{:+:, ::, CNil, Coproduct, HList, HNil, Inl, Inr, LabelledGeneric, Lazy, Witness}

import java.util.stream.Stream
import java.util.{Optional, stream}
import scala.jdk.OptionConverters.{RichOption, RichOptional}

/**
 * Created by MachineMuse on 2/8/2021.
 */
// Implicit codec generators for product and sum types (case classes and sealed traits)
object GenCodecsByName extends Logging {
  import VanillaCodecs._
  // Trait to tag case classes with so they can have codecs generated implicitly based on their parameter names
  trait CodecByName

  // Codec for the 'type' field
  private final val TYPENAMECODEC = STRINGCODEC.fieldOf("type")

  // Helper trait to generate codecs for optional fields in products and coproducts
  sealed trait Optionalizer[K, V] {
    def genField: MapCodec[V]
  }
  implicit def OptionalOptionalizer[K <: Symbol, V](implicit witness: Witness.Aux[K], codec: Codec[V]) = new Optionalizer[K, Optional[V]] {
    override val genField: MapCodec[Optional[V]] = codec.optionalFieldOf(witness.value.name)
  }
  implicit def OptionOptionalizer[K <: Symbol, V](implicit witness: Witness.Aux[K], codec: Codec[V]) = new Optionalizer[K, Option[V]] {
    override val genField: MapCodec[Option[V]] = codec.optionalFieldOf(witness.value.name).xmap(_.toScala, _.toJava)
  }
  implicit def NonOptionOptionalizer[K <: Symbol, V](implicit witness: Witness.Aux[K], codec: Codec[V]) = new Optionalizer[K, V] {
    override val genField: MapCodec[V] = codec.fieldOf(witness.value.name)
  }

  implicit val HNilHasMapCodecByName: MapCodec[HNil] = Codec.EMPTY.xmap(_ => HNil, _ => util.Unit.INSTANCE)

  implicit def HConsHasMapCodecByName[K <: Symbol, V, TailVals <: HList]
  (implicit
   witness: Witness.Aux[K],
   headCodec: Lazy[Optionalizer[K, V]],
   tailCodec: MapCodec[TailVals]
  ): MapCodec[FieldType[K, V] :: TailVals] = {
    val mapCodecHead = headCodec.value.genField
    val newCodec = MapCodec.of[FieldType[K, V] :: TailVals](
      new MapEncoder.Implementation[FieldType[K, V] :: TailVals] {
        override def encode[T](input: FieldType[K, V] :: TailVals, ops: DynamicOps[T], prefix: RecordBuilder[T]): RecordBuilder[T] =
          tailCodec.encode(input.tail, ops, mapCodecHead.encode(input.head, ops, prefix))

        override def keys[T](ops: DynamicOps[T]): stream.Stream[T] =
          Stream.concat(mapCodecHead.keys(ops), tailCodec.keys(ops))
      },
      new MapDecoder.Implementation[FieldType[K, V] :: TailVals] {
        override def decode[T](ops: DynamicOps[T], input: MapLike[T]): DataResult[FieldType[K, V] :: TailVals] =
          tailCodec.decode(ops, input).flatMap(tailVals => mapCodecHead.decode(ops, input).map(headVal => field[K](headVal) :: tailVals))

        override def keys[T](ops: DynamicOps[T]): stream.Stream[T] =
          Stream.concat(mapCodecHead.keys(ops), tailCodec.keys(ops))
      }
    )
    newCodec
  }


  implicit val CNilHasMapCodecByName: MapCodec[CNil] = MapCodec.of[CNil](
    new MapEncoder.Implementation[CNil] {
      override def encode[T](input: CNil, ops: DynamicOps[T], prefix: RecordBuilder[T]): RecordBuilder[T] = EMPTYCODEC.encode(util.Unit.INSTANCE, ops, prefix)

      override def keys[T](ops: DynamicOps[T]): Stream[T] = Stream.empty()
    },
    new MapDecoder.Implementation[CNil] {
      override def decode[T](ops: DynamicOps[T], input: MapLike[T]): DataResult[CNil] = DataResult.error("Invalid type tag for sum type: " + input)

      override def keys[T](ops: DynamicOps[T]): Stream[T] = Stream.empty()
    }
  )

  implicit def CConsHasMapCodecByName[K <: Symbol, V, Tail <: Coproduct](implicit witness: Witness.Aux[K], headCodec: Lazy[MapCodec[V]], tailCodec: MapCodec[Tail]): MapCodec[FieldType[K, V] :+: Tail] =
    MapCodec.of[FieldType[K, V] :+: Tail](
      new MapEncoder.Implementation[FieldType[K, V] :+: Tail] {
        override def encode[T](input: FieldType[K, V] :+: Tail, ops: DynamicOps[T], prefix: RecordBuilder[T]): RecordBuilder[T] =
          input match {
            case Inl(h) => TYPENAMECODEC.encode(witness.value.name, ops, headCodec.value.encode(h, ops, prefix))
            case Inr(t) => tailCodec.encode(t, ops, prefix)
          }

        override def keys[T](ops: DynamicOps[T]): Stream[T] = Stream.concat(headCodec.value.keys(ops), tailCodec.keys(ops))
      },
      new MapDecoder.Implementation[FieldType[K, V] :+: Tail] {
        override def decode[T](ops: DynamicOps[T], input: MapLike[T]): DataResult[FieldType[K, V] :+: Tail] =
          TYPENAMECODEC.decode(ops, input).flatMap(typ =>
            if (typ == witness.value.name)
              headCodec.value.decode(ops, input).map(curr => Inl(field[K](curr)))
            else
              tailCodec.decode(ops, input).map(tail => Inr(tail))
          )

        override def keys[T](ops: DynamicOps[T]): Stream[T] = Stream.concat(headCodec.value.keys(ops), tailCodec.keys(ops))
      }
    )

  implicit def genMapCodecByName[T <: CodecByName, Repr] (implicit labelledGeneric: LabelledGeneric.Aux[T, Repr], codecGen: Lazy[MapCodec[Repr]]): MapCodec[T] =
    codecGen.value.xmap(repr => labelledGeneric.from(repr), p => labelledGeneric.to(p))

  implicit def MapCodecHasCodec[T <: CodecByName] (implicit mapCodec: MapCodec[T]): Codec[T] =
    mapCodec.codec()
}
