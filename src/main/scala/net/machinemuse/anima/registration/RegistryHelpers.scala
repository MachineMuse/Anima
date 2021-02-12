package net.machinemuse.anima
package registration

import registration.SimpleItems.AnimaCreativeGroup
import util.VanillaCodecs.ConvenientRecipeSerializer

import com.mojang.serialization.Codec
import net.minecraft.block.Block
import net.minecraft.entity._
import net.minecraft.inventory.container.{Container, ContainerType}
import net.minecraft.item._
import net.minecraft.item.crafting.{IRecipe, IRecipeSerializer}
import net.minecraft.particles.ParticleType
import net.minecraft.tileentity.{TileEntity, TileEntityType}
import net.minecraft.world.World
import net.minecraftforge.common.extensions.IForgeContainerType
import net.minecraftforge.fml.RegistryObject
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import net.minecraftforge.fml.network.IContainerFactory
import net.minecraftforge.registries._
import org.apache.logging.log4j.scala.Logging

import java.util.function.Supplier

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

  def regTE[T <: TileEntity](name: String, ctor: Supplier[T], validBlocks: Supplier[Block]*): ConvenientRegistryObject[TileEntityType[T]] = {
    logger.info(s"Registering TileEntityType $name")
    ConvenientRegistryObject(TILE_ENTITIES.register(name,
      () => {
        TileEntityType.Builder.create[T](ctor, validBlocks.map(_.get()): _*)
      }.build(null)
    ))
  }


  case class Damageable(maxDamage: Int, defaultMaxDamage: Int)
  case class Stackable(maxStackSize: Int)

  def DEFAULT_ITEM_PROPERTIES = new Item.Properties().group(AnimaCreativeGroup).maxStackSize(64)

  def regSimpleItem(name: String, props: Item.Properties = DEFAULT_ITEM_PROPERTIES): ConvenientRegistryObject[Item] = {
    ConvenientRegistryObject(ITEMS.register(name, () => new Item(props)))
  }

  def regSimpleBlockItem[B <: Block](name: String, blockRegister: RegistryObject[B], props: Item.Properties = DEFAULT_ITEM_PROPERTIES): ConvenientRegistryObject[BlockItem]= {
    ConvenientRegistryObject(ITEMS.register(name, () => new BlockItem(blockRegister.get(), props)))
  }

  def regNamedBlockItem[B <: Block](name: String, blockRegister: RegistryObject[B], props: Item.Properties = DEFAULT_ITEM_PROPERTIES): ConvenientRegistryObject[BlockItem]= {
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

  def regCreativeTab(item: () => RegistryObject[Item]) = new ItemGroup(implicitly[MODID]) {
    override def createIcon(): ItemStack = {
      item().get.getDefaultInstance
    }
  }

  case class ConvenientRegistryObject[T <: IForgeRegistryEntry[_]](registryObject: RegistryObject[T]) {
    def unapply = registryObject.get
    def apply() = registryObject.get
    def get = registryObject.get
    def supplier: Supplier[T] = () => registryObject.get
  }

}
