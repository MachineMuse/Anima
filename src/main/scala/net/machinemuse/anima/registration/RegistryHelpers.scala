package net.machinemuse.anima
package registration

import registration.SimpleItems.AnimaCreativeGroup
import util.VanillaCodecs.{ConvenientRecipeSerializer, SavedData, mkCapStorage}

import com.mojang.serialization.Codec
import net.minecraft.block.Block
import net.minecraft.entity._
import net.minecraft.inventory.container.{Container, ContainerType}
import net.minecraft.item._
import net.minecraft.item.crafting.{IRecipe, IRecipeSerializer}
import net.minecraft.nbt.{CompoundNBT, INBT}
import net.minecraft.particles.ParticleType
import net.minecraft.tileentity.{TileEntity, TileEntityType}
import net.minecraft.util.Direction
import net.minecraft.world.World
import net.minecraftforge.common.capabilities._
import net.minecraftforge.common.extensions.IForgeContainerType
import net.minecraftforge.common.util.{INBTSerializable, LazyOptional}
import net.minecraftforge.fml.RegistryObject
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import net.minecraftforge.fml.network.IContainerFactory
import net.minecraftforge.registries._
import org.apache.logging.log4j.scala.Logging

import java.util.concurrent.Callable
import java.util.function.Supplier
import scala.reflect.ClassTag

/**
 * Created by MachineMuse on 1/28/2021.
 */
object RegistryHelpers extends Logging {

  def mkRegister[T <: IForgeRegistryEntry[T], RT <: IForgeRegistry[T]](registry: RT): DeferredRegister[T] = {
    DeferredRegister.create[T](registry, implicitly[MODID]).andDo(_.register(FMLJavaModLoadingContext.get.getModEventBus))
  }

  val ITEMS: DeferredRegister[Item] = mkRegister(ForgeRegistries.ITEMS)
  val BLOCKS: DeferredRegister[Block] = mkRegister(ForgeRegistries.BLOCKS)
  val CONTAINERS: DeferredRegister[ContainerType[_]] = mkRegister(ForgeRegistries.CONTAINERS)
  val TILE_ENTITIES: DeferredRegister[TileEntityType[_]] = mkRegister(ForgeRegistries.TILE_ENTITIES)
  val ENTITIES: DeferredRegister[EntityType[_]] = mkRegister(ForgeRegistries.ENTITIES)
  val RECIPE_SERIALIZERS: DeferredRegister[IRecipeSerializer[_]] = mkRegister(ForgeRegistries.RECIPE_SERIALIZERS)
  val PARTICLES: DeferredRegister[ParticleType[_]] = mkRegister(ForgeRegistries.PARTICLE_TYPES)

  def regRecipeSerializer[R <: IRecipe[_]](name: String, codec: Codec[R], default: R): ConvenientRegistryObject[IRecipeSerializer[R]] = ConvenientRegistryObject(RECIPE_SERIALIZERS.register(name, () => codec.mkSerializer(default)))

  def regEntityType[E <: Entity](name: String, initializer: () => Unit, ctor: (EntityType[E], World) => E, classification:EntityClassification): ConvenientRegistryObject[EntityType[E]] = {
    logger.info(s"Registering EntityType $name")
    ConvenientRegistryObject(ENTITIES.register(name, () => EntityType.Builder.create(ctor.butFirst(initializer)(_,_), classification).setShouldReceiveVelocityUpdates(false).build(name)))
  }

  def regTE[T <: TileEntity](name: String, ctor: Supplier[T], validBlocks: Supplier[_ <: Block]*): ConvenientRegistryObject[TileEntityType[T]] = {
    logger.info(s"Registering TileEntityType $name")
    ConvenientRegistryObject(TILE_ENTITIES.register(name,
      () => {
        TileEntityType.Builder.create[T](ctor, validBlocks.map(_.get()): _*)
      }.build(null)
    ))
  }

  def regCapWithStorage[D, T <: SavedData[D]](ctor: Callable[T])(implicit tag: ClassTag[T], codec: Codec[D]) = {
    CapabilityManager.INSTANCE.register[T](tag.runtimeClass.asInstanceOf[Class[T]], mkCapStorage[D, T], ctor)
  }

  def mkCapabilityProviderWithSaveData[I <: SavedData[_]](capability: Capability[I], interface: () => I) =
    new ICapabilityProvider with INBTSerializable[INBT] {
      private val localCapInst = interface()

      override def getCapability[T](cap: Capability[T], side: Direction): LazyOptional[T] =
        capability.orEmpty[T](cap, LazyOptional.of[I](() => localCapInst))

      override def serializeNBT(): INBT = capability.writeNBT(localCapInst, Direction.DOWN)

      override def deserializeNBT(nbt: INBT): Unit = capability.readNBT(localCapInst, Direction.DOWN, nbt)
    }

  def mkUnsavedCapabilityProvider[I](capability: Capability[I], interface: () => I) =
    new ICapabilityProvider with INBTSerializable[INBT] {
      private val localCapInst = interface()

      override def getCapability[T](cap: Capability[T], side: Direction): LazyOptional[T] =
        capability.orEmpty[T](cap, LazyOptional.of[I](() => localCapInst))

      override def serializeNBT(): INBT = new CompoundNBT()

      override def deserializeNBT(nbt: INBT): Unit = ()
    }

  case class Damageable(maxDamage: Int, defaultMaxDamage: Int)
  case class Stackable(maxStackSize: Int)

  def DEFAULT_ITEM_PROPERTIES = new Item.Properties().group(AnimaCreativeGroup).maxStackSize(64)

  def regSimpleItem(name: String, props: Item.Properties = DEFAULT_ITEM_PROPERTIES): ConvenientRegistryObject[Item] = {
    ConvenientRegistryObject(ITEMS.register(name, () => new Item(props)))
  }

  def regSimpleBlockItem[B <: Block](name: String, blockRegister: Supplier[B], props: Item.Properties = DEFAULT_ITEM_PROPERTIES): ConvenientRegistryObject[BlockItem]= {
    ConvenientRegistryObject(ITEMS.register(name, () => new BlockItem(blockRegister.get(), props)))
  }

  def regNamedBlockItem[B <: Block](name: String, blockRegister: Supplier[B], props: Item.Properties = DEFAULT_ITEM_PROPERTIES): ConvenientRegistryObject[BlockItem]= {
    ConvenientRegistryObject(ITEMS.register(name, () => new BlockNamedItem(blockRegister.get(), props)))
  }

  def regExtendedItem[I <: Item](name: String, supp: Supplier[I]): ConvenientRegistryObject[I] = {
    ConvenientRegistryObject(ITEMS.register(name, supp))
  }

  def regBlock[B <: Block](name: String, supp: Supplier[B]): ConvenientRegistryObject[B] = {
    ConvenientRegistryObject(BLOCKS.register(name, supp))
  }

  def regContainerType[C <: Container](name: String, fac: IContainerFactory[C]): ConvenientRegistryObject[ContainerType[C]] = {
    ConvenientRegistryObject(CONTAINERS.register(name, () => IForgeContainerType.create(fac)))
  }

  def regCreativeTab(item: () => Supplier[Item]) = new ItemGroup(implicitly[MODID]) {
    override def createIcon(): ItemStack = {
      item().get.getDefaultInstance
    }
  }

  case class ConvenientRegistryObject[T <: IForgeRegistryEntry[_]](registryObject: RegistryObject[T]) extends Supplier[T] {
    final def unapply(obj: T): Option[T] = if(obj == registryObject.get) obj.some else none[T]
    final def apply(): T = registryObject.get
    final def get: T = registryObject.get
    final def supplier: Supplier[T] = () => registryObject.get
  }

}
