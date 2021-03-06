package net.machinemuse.anima
package registration

import com.google.gson.JsonObject
import com.mojang.serialization.Codec
import net.minecraft.block.Block
import net.minecraft.entity._
import net.minecraft.inventory.container.{Container, ContainerType}
import net.minecraft.item._
import net.minecraft.item.crafting.{IRecipe, IRecipeSerializer}
import net.minecraft.loot.LootContext
import net.minecraft.loot.conditions.ILootCondition
import net.minecraft.nbt.{CompoundNBT, INBT}
import net.minecraft.particles.ParticleType
import net.minecraft.tileentity.{TileEntity, TileEntityType}
import net.minecraft.util.{Direction, ResourceLocation}
import net.minecraft.world.World
import net.minecraftforge.common.capabilities._
import net.minecraftforge.common.extensions.IForgeContainerType
import net.minecraftforge.common.loot.{GlobalLootModifierSerializer, LootModifier}
import net.minecraftforge.common.util.{INBTSerializable, LazyOptional}
import net.minecraftforge.fml.RegistryObject
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import net.minecraftforge.fml.network.IContainerFactory
import net.minecraftforge.registries._

import java.util.concurrent.Callable
import java.util.function.Supplier
import scala.reflect.ClassTag

import registration.SimpleItems.AnimaCreativeGroup
import util.Logging
import util.VanillaCodecs.{ConvenientCodec, ConvenientRecipeSerializer, SavedData, mkCapStorage}
import java.util

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
  val LOOT_MODIFIER_SERIALIZERS: DeferredRegister[GlobalLootModifierSerializer[_]] = mkRegister(ForgeRegistries.LOOT_MODIFIER_SERIALIZERS)

  def mkSimpleLootModifier[D : Codec](data: D, conditions: Array[ILootCondition])(f: (util.List[ItemStack], LootContext) => util.List[ItemStack]) =
    new SimpleLootModifier[D](data, conditions) {
      override def doApply(generatedLoot: util.List[ItemStack], context: LootContext): util.List[ItemStack] = {
        f(generatedLoot, context)
      }
    }

  abstract class SimpleLootModifier[D](val data: D, conditions: Array[ILootCondition]) extends LootModifier(conditions){
    def getConditions = conditions
  }

  def regLootModifierSerializer[D, T <: SimpleLootModifier[D]](name: String, ctor: (D, Array[ILootCondition]) => T) (implicit codec: Codec[D])
  : ConvenientRegistryObject[GlobalLootModifierSerializer[SimpleLootModifier[D]]] =
    ConvenientRegistryObject(LOOT_MODIFIER_SERIALIZERS.register(name, () => mkSimpleLootSerializer(codec, ctor)))

  def mkSimpleLootSerializer[D, T <: SimpleLootModifier[D]](implicit codec: Codec[D], ctor: (D, Array[ILootCondition]) => T) = new GlobalLootModifierSerializer[SimpleLootModifier[D]] {
    override def read(location: ResourceLocation, json: JsonObject, lootconditions: Array[ILootCondition]): SimpleLootModifier[D] = {
      val data = codec.parseJson(json.get("data")).get // OrElse{logger.error(s"Couldn't deserialize ${json.get("data"); ???} to a simple loot modifier")}
      ctor(data, lootconditions)
    }

    override def write(instance: SimpleLootModifier[D]): JsonObject = {
      val conditionsContainerJson = makeConditions(instance.getConditions)
      conditionsContainerJson.add("data", codec.writeJson(instance.data))
      conditionsContainerJson
    }
  }

  def regRecipeSerializer[R <: IRecipe[_]](name: String, codec: Codec[R], default: R): ConvenientRegistryObject[IRecipeSerializer[R]] = ConvenientRegistryObject(RECIPE_SERIALIZERS.register(name, () => codec.mkRecipeSerializer(default)))

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
