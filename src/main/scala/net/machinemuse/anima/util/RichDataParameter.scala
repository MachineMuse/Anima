package net.machinemuse.anima
package util

import net.minecraft.block.BlockState
import net.minecraft.entity._
import net.minecraft.entity.merchant.villager.VillagerData
import net.minecraft.item.ItemStack
import net.minecraft.nbt.{CompoundNBT, NBTDynamicOps}
import net.minecraft.network.IPacket
import net.minecraft.network.datasync._
import net.minecraft.particles.IParticleData
import net.minecraft.util.math.{BlockPos, Rotations}
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.{Unit => _, _}
import net.minecraftforge.fml.network.NetworkHooks

import java.util._
import scala.collection.mutable

import util.RichDataParameter.ParameterTypes.ParameterType

/**
 * Created by MachineMuse on 1/26/2021.
 */
object RichDataParameter extends Logging {

  // Trait for the class to extend for this package's boilerplate reduction
  trait DataHandlingEntity extends Entity {
    def registrar: ParameterRegistrar // has to be a def when overriding because it's called before the constructor can initialize it
    def mkDataSync[T: ParameterType](name: String, default: T) = DataSync(registrar.mkDataParameter(name, default), getDataManager)

    override def writeAdditional(compound: CompoundNBT): Unit = registrar.parameterRegistry.values.foreach { _.write(this.getDataManager, compound) }
    override def readAdditional(compound: CompoundNBT): Unit = registrar.parameterRegistry.values.foreach { _.read(this.getDataManager, compound) }

    override def registerData(): Unit = {}
    override def createSpawnPacket(): IPacket[_] = NetworkHooks.getEntitySpawningPacket(this)
  }

  trait DataHandlingLivingEntity extends LivingEntity with DataHandlingEntity {
    // LivingEntity overrides many of these abstract methods with important stuff so we need to call super(), cant do this by default because theyre abstract
    override def registerData(): Unit = {
      super.registerData()
    }
    override def writeAdditional(compound: CompoundNBT): Unit = {
      super.writeAdditional(compound)
      registrar.parameterRegistry.values.foreach { _.write(this.getDataManager, compound) }
    }
    override def readAdditional(compound: CompoundNBT): Unit = {
      super.readAdditional(compound)
      registrar.parameterRegistry.values.foreach { _.read(this.getDataManager, compound) }
    }

    // TODO: test and make sure this works
    override def createSpawnPacket(): IPacket[_] = {
      NetworkHooks.getEntitySpawningPacket(this)
    }
  }


  // Trait for the class's companion object to extend
  class ParameterRegistrar(clazz: Class[_ <: Entity]) {
    import ParameterTypes._ // Get those implicits in scope
    val parameterRegistry = new mutable.HashMap[String, ParameterInstance[_]]()
    final def mkDataParameter[T](name: String, default: T)(implicit pt: ParameterType[T]): ParameterInstance[T] = {
      // this is technically unsafe, but since it can only be accessed this way, it will only cause problems if you register
      // two different data parameters with the same name.
      parameterRegistry.getOrElseUpdate(name, pt.mkInstance(name, clazz, default)).asInstanceOf[ParameterInstance[T]]
    }
  }

  // Case class for accessing a data parameter.
  case class DataSync[T](param: ParameterInstance[T], dm: EntityDataManager) {
    param.register(dm)
    final def get: T = param.get(dm)
    final def set(value: T): Unit = param.set(value)(dm)
    final def :=(that: T): T = that.andDo(result => set(result))
    final def apply() = get
  }
  implicit class DataSyncNumeric[T](data: DataSync[T])(implicit numeric: Numeric[T]) {
    import numeric._
    final def +(that: T): T = data.get + that
    final def +=(that: T): T = (this + that).andDo(result => data.set(result))
    final def -(that: T): T = data.get - that
    final def -=(that: T): T = (this - that).andDo(result => data.set(result))
    final def *(that: T): T = data.get * that
    final def *=(that: T): T = (this * that).andDo(result => data.set(result))
    final def unary_- : T = -data.get
    final def unary_++ : T = this += numeric.one
    final def unary_-- : T = this -= numeric.one
  }

  trait ParameterInstance[T] {
    def register(implicit dm: EntityDataManager): Unit

    def get(implicit dm: EntityDataManager): T
    def set(value: T) (implicit dm: EntityDataManager): Unit

    def write(dm: EntityDataManager, compound: CompoundNBT): Unit
    def read(dm: EntityDataManager, compound: CompoundNBT): Unit
  }

  case class VanillaParameterInstance[T](name: String, key: DataParameter[T], default: T, compoundWriter: (String, CompoundNBT, T) => Unit, compoundReader: (String, CompoundNBT) => T) extends ParameterInstance[T] {
    def register(implicit dm: EntityDataManager): Unit = dm.register(key, default)

    def get(implicit dm: EntityDataManager): T = dm.get(key)
    def set(value: T) (implicit dm: EntityDataManager): Unit = dm.set(key, value)

    def write(dm: EntityDataManager, compound: CompoundNBT): Unit = compoundWriter(name, compound, get(dm))
    def read(dm: EntityDataManager, compound: CompoundNBT): Unit = set (compoundReader(name, compound))(dm)
  }

  case class ExtendedParameterInstance[T, D](name: String, key: DataParameter[D], default: T, compoundWriter: (String, CompoundNBT, T) => Unit, compoundReader: (String, CompoundNBT) => T, encode: T => D, decode: D => T) extends ParameterInstance[T] {
    def register(implicit dm: EntityDataManager): Unit = dm.register(key, encode(default))

    def get(implicit dm: EntityDataManager): T = decode(dm.get(key))
    def set(value: T) (implicit dm: EntityDataManager): Unit = dm.set(key, encode(value))

    def write(dm: EntityDataManager, compound: CompoundNBT): Unit = compoundWriter(name, compound, get(dm))
    def read(dm: EntityDataManager, compound: CompoundNBT): Unit = set (compoundReader(name, compound))(dm)
  }

  case class UnsavedParameterInstance[T](name: String, key: DataParameter[T], default: T) extends ParameterInstance[T] {
    def register(implicit dm: EntityDataManager): Unit = dm.register(key, default)

    def get(implicit dm: EntityDataManager): T = dm.get(key)
    def set(value: T) (implicit dm: EntityDataManager): Unit = dm.set(key, value)

    def write(dm: EntityDataManager, compound: CompoundNBT): Unit = ()
    def read(dm: EntityDataManager, compound: CompoundNBT): Unit = ()
  }

  object ParameterTypes {
    trait ParameterType[T] {
      def mkInstance(name: String, clazz: Class[_ <: Entity], default: T): ParameterInstance[T]
    }
    case class VanillaParameterType[T](serializer: IDataSerializer[T], compoundWriter: (String, CompoundNBT, T) => Unit, compoundReader: (String, CompoundNBT) => T) extends ParameterType[T] {
      override def mkInstance(name: String, clazz: Class[_ <: Entity], default: T) = VanillaParameterInstance(name, EntityDataManager.createKey(clazz, serializer), default, compoundWriter, compoundReader)
    }
    case class ExtendedParameterType[T, D](serializer: IDataSerializer[D], compoundWriter: (String, CompoundNBT, T) => Unit, compoundReader: (String, CompoundNBT) => T, encode: T => D, decode: D => T) extends ParameterType[T] {
      override def mkInstance(name: String, clazz: Class[_ <: Entity], default: T) = ExtendedParameterInstance(name, EntityDataManager.createKey(clazz, serializer), default, compoundWriter, compoundReader, encode, decode)
    }
    case class UnsavedParameterType[T](serializer: IDataSerializer[T]) extends ParameterType[T] {
      override def mkInstance(name: String, clazz: Class[_ <: Entity], default: T) = UnsavedParameterInstance(name, EntityDataManager.createKey(clazz, serializer), default)
    }

    implicit val SINT: ExtendedParameterType[Int, Integer] = ExtendedParameterType(
      DataSerializers.VARINT,
      (name, compound, value) => compound.putInt(name, value),
      (name, compound) => compound.getInt(name),
      new Integer(_),
      _.intValue()
    )

    implicit val BYTE:          VanillaParameterType[java.lang.Byte] = VanillaParameterType(
        DataSerializers.BYTE,
        (name, compound, value) => compound.putByte(name, value),
        (name, compound) => compound.getByte(name)
      )
    implicit val VARINT:        VanillaParameterType[Integer] = VanillaParameterType(
        DataSerializers.VARINT,
        (name, compound, value) => compound.putInt(name, value),
        (name, compound) => compound.getInt(name)
      )
    implicit val O_VARINT:      VanillaParameterType[OptionalInt] = VanillaParameterType(
        DataSerializers.OPTIONAL_VARINT,
        (name, compound, value) => if(value.isPresent) { compound.putInt(name, value.getAsInt) },
        (name, compound) => if(compound.contains(name, 3)) OptionalInt.of(compound.getInt(name)) else OptionalInt.empty()
      )
    implicit val FLOAT:         VanillaParameterType[java.lang.Float] = VanillaParameterType(
        DataSerializers.FLOAT,
        (name, compound, value) => compound.putFloat(name, value),
        (name, compound) => compound.getFloat(name)
      )
    implicit val STRING:        VanillaParameterType[String] = VanillaParameterType(
        DataSerializers.STRING,
        (name, compound, value) => compound.putString(name, value),
        (name, compound) => compound.getString(name)
      )
    implicit val ITEMSTACK:     VanillaParameterType[ItemStack] = VanillaParameterType(
        DataSerializers.ITEMSTACK,
        (name, compound, value) => compound.put(name, value.write(new CompoundNBT())),
        (name, compound) => ItemStack.read(compound.getCompound(name)
      ))
    implicit val BOOLEAN:       VanillaParameterType[java.lang.Boolean] = VanillaParameterType(
        DataSerializers.BOOLEAN,
        (name, compound, value) => compound.putBoolean(name, value),
        (name, compound) => compound.getBoolean(name)
      )
    implicit val ROTATIONS:     VanillaParameterType[Rotations] = VanillaParameterType(
        DataSerializers.ROTATIONS,
        (name, compound, value) => compound.put(name, value.writeToNBT()),
        (name, compound) => new Rotations(compound.getList(name, 5)) // 5 = float. TODO: confirm/test
      )
    implicit val BLOCK_POS:     VanillaParameterType[BlockPos] = VanillaParameterType(
        DataSerializers.BLOCK_POS,
        (name, compound, value) => compound.putLong(name, value.toLong),
        (name, compound) => BlockPos.fromLong(compound.getLong(name)
      ))
    implicit val O_BLOCK_POS:   VanillaParameterType[Optional[BlockPos]] = VanillaParameterType(
        DataSerializers.OPTIONAL_BLOCK_POS,
        (name, compound, value: Optional[BlockPos]) => if(value.isPresent) compound.putLong(name, value.get().toLong),
        (name, compound) => if(compound.contains(name)) Optional.of(BlockPos.fromLong(compound.getLong(name))) else Optional.empty()
      )
    implicit val DIRECTION:     VanillaParameterType[Direction] = VanillaParameterType(
        DataSerializers.DIRECTION,
        (name, compound, value) => compound.putInt(name, value.getIndex),
        (name, compound) => Direction.byIndex(compound.getInt(name))
      )
    implicit val O_UNIQUE_ID:   VanillaParameterType[Optional[UUID]] = VanillaParameterType(
        DataSerializers.OPTIONAL_UNIQUE_ID,
        (name, compound, value) => if(value.isPresent) compound.putUniqueId(name, value.get()),
        (name, compound) => if(compound.contains(name)) Optional.of(compound.getUniqueId(name)) else Optional.empty()
      )
    implicit val COMPOUND_NBT:  VanillaParameterType[CompoundNBT] = VanillaParameterType(
        DataSerializers.COMPOUND_NBT,
        (name, compound, value) => compound.put(name, value),
        (name, compound) => compound.getCompound(name)
      )
    implicit val TEXTCOMP:      VanillaParameterType[ITextComponent] = VanillaParameterType(
      DataSerializers.TEXT_COMPONENT,
      (name, compound, value) => compound.putString(name, ITextComponent.Serializer.toJson(value)),
      (name, compound) => ITextComponent.Serializer.getComponentFromJson(compound.getString(name))
    )
    implicit val O_TEXTCOMP:    VanillaParameterType[Optional[ITextComponent]] = VanillaParameterType(
      DataSerializers.OPTIONAL_TEXT_COMPONENT,
      (name, compound, value) => if(value.isPresent) { compound.putString(name, ITextComponent.Serializer.toJson(value.get)) },
      (name, compound) => if(compound.contains(name)) Optional.of(ITextComponent.Serializer.getComponentFromJson(compound.getString(name))) else Optional.empty()
    )
    implicit val POSE:          ParameterType[Pose] =                 VanillaParameterType(
      DataSerializers.POSE,
      (name, compound, value) => compound.putInt(name, value.ordinal()),
      (name, compound) => Pose.values()(compound.getInt(name))
    )
    implicit val O_BLOCK_STATE: ParameterType[Optional[BlockState]] = VanillaParameterType(
      DataSerializers.OPTIONAL_BLOCK_STATE,
      (name, compound, value) => if(value.isPresent) { compound.put(name, BlockState.CODEC.encodeStart(NBTDynamicOps.INSTANCE, value.get).result().get()) },
      (name, compound) => BlockState.CODEC.parse(NBTDynamicOps.INSTANCE, compound.get(name)).result()
    )
    implicit val VILLAGER_DATA: ParameterType[VillagerData] =         VanillaParameterType(
      DataSerializers.VILLAGER_DATA,
      (name, compound, value) => compound.put(name, VillagerData.CODEC.encodeStart(NBTDynamicOps.INSTANCE, value).result().get()),
      (name, compound) => VillagerData.CODEC.parse(NBTDynamicOps.INSTANCE, compound.get(name)).resultOrPartial(errStr => logger.error(s"Problem loading villager data: $errStr")).get()
    )
    // TODO: NBT serializers/deserializers for these ones if deemed necessary
    implicit val PARTICLE_DATA: ParameterType[IParticleData] =        UnsavedParameterType(DataSerializers.PARTICLE_DATA)
  }
}
