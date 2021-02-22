package net.machinemuse.anima
package util

import com.google.gson._
import com.mojang.brigadier.StringReader
import com.mojang.datafixers.util.Pair
import com.mojang.datafixers.{util => du}
import com.mojang.serialization
import com.mojang.serialization._
import com.mojang.serialization.codecs._
import io.netty.buffer._
import net.minecraft.block.Block
import net.minecraft.entity.EntityType
import net.minecraft.entity.ai.attributes.Attribute
import net.minecraft.item.Item
import net.minecraft.item.crafting.{IRecipe, IRecipeSerializer}
import net.minecraft.nbt._
import net.minecraft.network.PacketBuffer
import net.minecraft.particles.{IParticleData, ParticleType}
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.vector._
import net.minecraft.util.registry.Registry
import net.minecraft.{util => mu}
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.registries.ForgeRegistryEntry

import java.io._
import java.nio.ByteBuffer
import java.{util => ju}
import scala.annotation.nowarn
import scala.collection.mutable
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters.RichOptional

/**
 * Created by MachineMuse on 2/6/2021.
 */
object VanillaCodecs extends Logging {
  // Primitives
  // TODO: see if you can circumvent boxing/unboxing via scala primitive generic handling, or whether it's even worth bothering
  // in case the compiler optimizes it out anyway

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
  implicit val INT_STREAMCODEC: Codec[ju.stream.IntStream] = Codec.INT_STREAM
  implicit val BYTE_BUFFERCODEC: Codec[ByteBuffer] = Codec.BYTE_BUFFER
  implicit val LONG_STREAMCODEC: Codec[ju.stream.LongStream] = Codec.LONG_STREAM

  implicit val EMPTYCODEC: MapCodec[du.Unit] = Codec.EMPTY
  implicit val SEMPTYCODEC: MapCodec[Unit] = Codec.EMPTY.xmap(_ => (), _ => du.Unit.INSTANCE)

  /*_*/ // Registries
  implicit val ITEMCODEC: Codec[Item] = Registry.ITEM : @nowarn
  implicit val BLOCKCODEC: Codec[Block] = Registry.BLOCK : @nowarn
  implicit val ATTRIBUTECODEC: Codec[Attribute] = Registry.ATTRIBUTE : @nowarn

  // Caution: This will fail to generate a codec at the top level, but it works fine if it's an encased type
  // such as Option[EntityType[_]] or List[EntityType[_]]
  implicit val ENTITYTYPECODEC: Codec[EntityType[_]] = Registry.ENTITY_TYPE : @nowarn
  /*_*/ //etc...

  // Generics
  implicit def LISTCODEC[A: Codec](implicit ca: Codec[A]): Codec[java.util.List[A]] = new ListCodec(ca)
  implicit def SLISTCODEC[A: Codec](implicit ca: Codec[A]): Codec[List[A]] = new ListCodec(ca).xmap (_.asScala.toList, _.asJava)
  implicit def SETCODEC[A: Codec](implicit ca: Codec[A]): Codec[Set[A]] = new ListCodec(ca).xmap (_.asScala.toSet, set => new ju.ArrayList[A](set.asJava))

  implicit def PAIRCODEC[A: Codec, B: Codec]: Codec[Pair[A, B]] = new PairCodec(implicitly[Codec[A]], implicitly[Codec[B]])
  implicit def SPAIRCODEC[A: Codec, B: Codec]: Codec[(A, B)] = PAIRCODEC[A,B].xmap (p => (p.getFirst, p.getSecond), t => Pair.of(t._1, t._2))


  // TODO: Revisit and see if a more robust and efficient string conversion for keys is possible
  val GSON = new Gson()
  implicit def STRINGMAPCODEC[V: Codec]: Codec[ju.Map[String, V]] = Codec.unboundedMap(
    STRINGCODEC, implicitly[Codec[V]]
  )
  implicit def LONGMAPCODEC[V: Codec]: Codec[ju.Map[Long, V]] = Codec.unboundedMap(
    STRINGCODEC.xmap(_.toLong, _.toString), implicitly[Codec[V]]
  )

  implicit def KMAPCODEC[K: Codec, V: Codec]: Codec[ju.Map[K,V]] = Codec.unboundedMap(
    STRINGCODEC.flatXmap(
      str => implicitly[Codec[K]].parse(JsonOps.INSTANCE, new JsonParser().parse(str)),
      k => DataResult.success(GSON.toJson(implicitly[Codec[K]].writeJson(k)))
    ), implicitly[Codec[V]]
  )
  implicit def SMAPCODEC[K: Codec, V: Codec](implicit jmapcodec: Codec[ju.Map[K, V]]): Codec[mutable.Map[K, V]] = jmapcodec.xmap(_.asScala, _.asJava)

  implicit def MATCHINGTRIPLETCODEC[A: Codec]: Codec[(A, A, A)] = SLISTCODEC[A].flatXmap ({
      case List(x,y,z) => DataResult.success((x,y,z))
      case el => DataResult.error(s"Triplet didn't contain 3 elements: $el")
    },{
      case (x,y,z) => DataResult.success(List(x,y,z))
    })

  implicit def MATCHINGQUADCODEC[A: Codec]: Codec[(A, A, A, A)] = SLISTCODEC[A].flatXmap ({
    case List(w,x,y,z) => DataResult.success((w,x,y,z))
    case el => DataResult.error(s"Quad didn't contain 4 elements: $el")
  },{
    case (w,x,y,z) => DataResult.success(List(w,x,y,z))
  })
  //more?

  // Vectors
  implicit def VECTOR2FCODEC  : Codec[Vector2f] =   PAIRCODEC[Float, Float]     .xmap(t => new Vector2f(t.getFirst, t.getSecond), v => Pair.of(v.x, v.y))
  implicit def VECTOR3DCODEC  : Codec[Vector3d] =   MATCHINGTRIPLETCODEC[Double].xmap(t => new Vector3d(t._1,t._2,t._3), v => (v.getX, v.getY, v.getZ))
  implicit def VECTOR3ICODEC  : Codec[Vector3i] =   MATCHINGTRIPLETCODEC[Int]   .xmap(t => new Vector3i(t._1,t._2,t._3), v => (v.getX, v.getY, v.getZ))
  implicit def VECTOR3FCODEC  : Codec[Vector3f] =   MATCHINGTRIPLETCODEC[Float] .xmap(t => new Vector3f(t._1,t._2,t._3), v => (v.getX, v.getY, v.getZ))
  implicit def VECTOR4FCODEC  : Codec[Vector4f] =   MATCHINGQUADCODEC[Float]    .xmap(t => new Vector4f(t._1,t._2,t._3,t._4), v => (v.getW, v.getX, v.getY, v.getZ))
  implicit def QUATERNIONCODEC: Codec[Quaternion] = MATCHINGQUADCODEC[Float]    .xmap(t => new Quaternion(t._1,t._2,t._3,t._4), v => (v.getW, v.getX, v.getY, v.getZ))

  implicit def BLOCKPOSCODEC : Codec[BlockPos] = LONGCODEC.xmap(t => BlockPos.fromLong(t), b => b.toLong)

  import JavaFunctionConverters._

  implicit class ConvenientCodec[A](codec: Codec[A]) {
    // Basic functionality
    def parseINBT(nbt: INBT): Option[A] = {
      codec.parse(NBTDynamicOps.INSTANCE, nbt)
        .resultOrPartial { err =>
          logger.error(s"Error while decoding input from stream: $err")
          logger.error(s" [input: $nbt ]")
        }.toScala
    }
    def writeINBT(obj: A): INBT = codec.encodeStart(NBTDynamicOps.INSTANCE, obj).getOrThrow(false, str => logger.error(s"Problem encoding: $str"))

    def parseJson(json: JsonElement): Option[A] = {
      codec.parse(JsonOps.INSTANCE, json)
        .resultOrPartial { err =>
          logger.error(s"Error while decoding input from stream: $err")
          logger.error(s" [input: $json ]")
        }.toScala
    }
    def writeJson(obj: A): JsonElement = codec.encodeStart(JsonOps.INSTANCE, obj).getOrThrow(false, str => logger.error(s"Problem encoding: $str"))
    def writeIntoMutableJson(obj: A, jsonOut: JsonObject): Unit = {
      val jsonIn = codec.writeJson(obj)
      jsonIn.getAsJsonObject.entrySet().forEach { entry =>
        jsonOut.add(entry.getKey, entry.getValue)
      }
    }

    // Helpers for compressed stream tools
    def mkDataCompound(nbt: INBT): CompoundNBT = {
      val data = new CompoundNBT
      data.put("data", nbt)
      data.putInt("DataVersion", mu.SharedConstants.getVersion.getWorldVersion)
      data
    }
    def mkDataCompound(obj: A): CompoundNBT = mkDataCompound(writeINBT(obj))
    def parseDataCompound(nbt: CompoundNBT): Option[A] = if(nbt.contains("data")) parseINBT(nbt.get("data")) else None

    // Using compressed stream tools
    def writeCompressed(output: OutputStream, obj: A): Unit = CompressedStreamTools.writeCompressed(mkDataCompound(writeINBT(obj)), output)
    def readCompressed(input: InputStream): Option[A] = parseDataCompound(CompressedStreamTools.readCompressed(input))
    def writeCompressed(output: File, obj: A): Unit = CompressedStreamTools.writeCompressed(mkDataCompound(writeINBT(obj)), output)
    def readCompressed(input: File): Option[A] = parseDataCompound(CompressedStreamTools.readCompressed(input))
    def writeCompressed(byteBuf: ByteBuf, obj: A): Unit = writeCompressed(new ByteBufOutputStream(byteBuf), obj)
    def readCompressed(byteBuf: ByteBuf): Option[A] = readCompressed(new ByteBufInputStream(byteBuf))

    def writeUncompressed(output: DataOutput, obj: A): Unit = CompressedStreamTools.write(mkDataCompound(writeINBT(obj)), output)
    def readUncompressed(input: DataInput): Option[A] = parseDataCompound(CompressedStreamTools.read(input))
    def writeUncompressed(output: File, obj: A): Unit = CompressedStreamTools.write(mkDataCompound(writeINBT(obj)), output)
    def readUncompressed(input: File): Option[A] = parseDataCompound(CompressedStreamTools.read(input))
    def writeUncompressed(output: ByteBuf, obj: A): Unit = writeUncompressed(new ByteBufOutputStream(output), obj)
    def readUncompressed(input: ByteBuf): Option[A] = readUncompressed(new ByteBufInputStream(input))

  }

  trait SavedData[D] {
    def getData: D
    def getCodec(implicit codec: Codec[D]): Codec[D] = codec
    def putData(data: D): Unit
  }

  def mkCapStorage[D, I <: SavedData[D]](implicit codec: Codec[D]) = new Capability.IStorage[I] {
    override def writeNBT(capability: Capability[I], instance: I, side: mu.Direction): INBT = {
      instance.getCodec.writeINBT(instance.getData)
    }

    override def readNBT(capability: Capability[I], instance: I, side: mu.Direction, nbt: INBT): Unit = {
      val maybeData = instance.getCodec.parseINBT(nbt)
      maybeData.foreach(data => instance.putData(data))
    }
  }

  // Helpers for assorted things that will probably use codecs in the future
  implicit class ConvenientParticleDataCodec[A <: IParticleData](codec: Codec[A]) {
    def deserializer = new IParticleData.IDeserializer[A] {
      override def deserialize(particleTypeIn: ParticleType[A], reader: StringReader): A = {
        reader.expect(' ')
        val string = reader.getRemaining
        val json = new JsonParser().parse(string)
        codec.parseJson(json).get // Unsafe for command but who knows maybe it handles exceptions gracefully
      }
      override def read(particleTypeIn: ParticleType[A], buffer: PacketBuffer): A = codec.readCompressed(buffer).get
    } : @nowarn

    def mkParticleType(alwaysShow: Boolean) = new ParticleType[A](alwaysShow, deserializer) {
      override def func_230522_e_(): Codec[A] = codec
    }
  }

  implicit class ConvenientRecipeSerializer[A <: IRecipe[_]](codec: Codec[A]) {
    def mkRecipeSerializer(default: A) = new ForgeRegistryEntry[IRecipeSerializer[_]] with IRecipeSerializer[A] {
      override def read(recipeId: mu.ResourceLocation, json: JsonObject): A = codec.parseJson(json).getOrElse(default)
      override def read(recipeId: mu.ResourceLocation, buffer: PacketBuffer): A = codec.readUncompressed(buffer).getOrElse(default)
      override def write(buffer: PacketBuffer, recipe: A): Unit = codec.writeUncompressed(buffer, recipe)
    }
  }

}
