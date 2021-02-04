package net.machinemuse.anima
package registration

import registration.SimpleItems.AnimaCreativeGroup

import net.minecraft.block.Block
import net.minecraft.entity._
import net.minecraft.inventory.container.{Container, ContainerType}
import net.minecraft.item._
import net.minecraft.item.crafting.IRecipeSerializer
import net.minecraft.tileentity.{TileEntity, TileEntityType}
import net.minecraft.world.World
import net.minecraftforge.common.ToolType
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
    DeferredRegister.create[T](registry, Anima.MODID).andDo(_.register(FMLJavaModLoadingContext.get.getModEventBus))
  }

  val ITEMS: DeferredRegister[Item] = mkRegister(ForgeRegistries.ITEMS)
  val BLOCKS: DeferredRegister[Block] = mkRegister(ForgeRegistries.BLOCKS)
  val CONTAINERS: DeferredRegister[ContainerType[_]] = mkRegister(ForgeRegistries.CONTAINERS)
  val TILE_ENTITIES: DeferredRegister[TileEntityType[_]] = mkRegister(ForgeRegistries.TILE_ENTITIES)
  val ENTITIES: DeferredRegister[EntityType[_]] = mkRegister(ForgeRegistries.ENTITIES)
  val RECIPE_SERIALIZERS: DeferredRegister[IRecipeSerializer[_]] = mkRegister(ForgeRegistries.RECIPE_SERIALIZERS)


  def regEntityType[E <: Entity](name: String, initializer: () => Unit, ctor: (EntityType[E], World) => E, classification:EntityClassification): RegistryObject[EntityType[E]] = {
    logger.info(s"Registering EntityType $name")
    ENTITIES.register(name, () => EntityType.Builder.create(ctor.butFirst(initializer)(_,_), classification).setShouldReceiveVelocityUpdates(false).build(name))
  }

  def regTE[T <: TileEntity](name: String, ctor: Supplier[T], validBlocks: Supplier[Block]*): RegistryObject[TileEntityType[T]] = {
    logger.info(s"Registering TileEntityType $name")
    TILE_ENTITIES.register(name,
      () => {
        TileEntityType.Builder.create[T](ctor, validBlocks.map(_.get()): _*)
      }.build(null)
    )
  }


  case class Damageable(maxDamage: Int, defaultMaxDamage: Int)
  case class Stackable(maxStackSize: Int)

  case class ItemProperties(creativeGroup: Option[ItemGroup] = None, food: Option[Food] = None,
                            stackOrDamage: Either[Damageable, Stackable] = Right(Stackable(64)),
                            fireImmune: Boolean = false, noRepair: Boolean = false,
                            toolTypes: Set[(ToolType, Int)] = Set.empty, rarity: Rarity = Rarity.COMMON) {
    def apply: Item.Properties = {
      val applyProps =
        creativeGroup.fold[Item.Properties => Item.Properties](_.group(AnimaCreativeGroup))(g => it => it.group(g)) andThen
          food.foldId[Item.Properties](_.food(_)) andThen
          (firstip => toolTypes.foldLeft(firstip) { case (ip, (typ, level)) => ip.addToolType(typ, level) }) andThen
          stackOrDamage.fold(m => { i: Item.Properties => i.maxDamage(m.maxDamage).defaultMaxDamage(m.defaultMaxDamage) }, s => { i: Item.Properties => i.maxStackSize(s.maxStackSize) }) andThen
          (if (fireImmune) _.isImmuneToFire else identity[Item.Properties]) andThen
          (if (noRepair) _.setNoRepair else identity[Item.Properties]) andThen
          (_.rarity(rarity))

      applyProps(new Item.Properties)
    }

  }

  def regSimpleItem(name: String, props: Option[ItemProperties] = None): RegistryObject[Item] = {
    val concreteprops = props.map(_.apply).getOrElse(ItemProperties().apply)
    ITEMS.register(name, () => new Item(concreteprops))
  }

  def regSimpleBlockItem[B <: Block](name: String, blockRegister: RegistryObject[B], props: Option[ItemProperties] = None): RegistryObject[BlockItem]= {
    val concreteprops = props.map(_.apply).getOrElse(new Item.Properties)
    ITEMS.register(name, () => new BlockItem(blockRegister.get(), concreteprops))
  }

  def regExtendedItem[I <: Item](name: String, supp: Supplier[I]): RegistryObject[I] = {
    ITEMS.register(name, supp)
  }

  def regBlock[B <: Block](name: String, supp: Supplier[B]): RegistryObject[B] = {
    BLOCKS.register(name, supp)
  }

  def regContainerType[C <: Container](name: String, fac: IContainerFactory[C]): RegistryObject[ContainerType[C]] = {
    CONTAINERS.register(name, () => IForgeContainerType.create(fac))
  }

  def regCreativeTab(item: () => RegistryObject[Item]) = new ItemGroup(Anima.MODID) {
    override def createIcon(): ItemStack = {
      item().get.getDefaultInstance
    }
  }

}
