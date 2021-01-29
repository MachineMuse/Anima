package net.machinemuse.anima.registration

import net.machinemuse.anima.Anima
import net.machinemuse.anima.registration.AnimaRegistry.AnimaCreativeGroup
import net.machinemuse.anima.util.ScalaShorthand._
import net.minecraft.block.Block
import net.minecraft.client.renderer.tileentity.ItemStackTileEntityRenderer
import net.minecraft.entity.{Entity, EntityClassification, EntityType}
import net.minecraft.inventory.container.{Container, ContainerType}
import net.minecraft.item.{Food, Item, ItemGroup, Rarity}
import net.minecraft.tileentity.{TileEntity, TileEntityType}
import net.minecraft.world.World
import net.minecraftforge.common.ToolType
import net.minecraftforge.common.extensions.IForgeContainerType
import net.minecraftforge.fml.RegistryObject
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import net.minecraftforge.fml.network.IContainerFactory
import net.minecraftforge.registries.{DeferredRegister, ForgeRegistries, IForgeRegistry, IForgeRegistryEntry}
import org.apache.logging.log4j.LogManager

import java.util.concurrent.Callable
import java.util.function.Supplier

/**
 * Created by MachineMuse on 1/28/2021.
 */
object RegistryHelpers {
  private val LOGGER = LogManager.getLogger

  def mkRegister[T <: IForgeRegistryEntry[T], RT <: IForgeRegistry[T]](registry: RT): DeferredRegister[T] = {
    DeferredRegister.create[T](registry, Anima.MODID).andDo(_.register(FMLJavaModLoadingContext.get.getModEventBus))
  }

  val ITEMS: DeferredRegister[Item] = mkRegister(ForgeRegistries.ITEMS)
  val BLOCKS: DeferredRegister[Block] = mkRegister(ForgeRegistries.BLOCKS)
  type RegO[O <: IForgeRegistryEntry[_]] = RegistryObject[O]
  val CONTAINERS: DeferredRegister[ContainerType[_]] = mkRegister(ForgeRegistries.CONTAINERS)
  type RegCT[C <: Container] = RegistryObject[ContainerType[C]]
  val TILE_ENTITIES: DeferredRegister[TileEntityType[_]] = mkRegister(ForgeRegistries.TILE_ENTITIES)
  type RegTE[T <: TileEntity] = RegistryObject[TileEntityType[T]]
  val ENTITIES: DeferredRegister[EntityType[_]] = mkRegister(ForgeRegistries.ENTITIES)
  type RegE[E <: Entity] = RegistryObject[EntityType[E]]



  def regEntity[E <: Entity](name: String, initializer: () => Unit, ctor: (EntityType[E], World) => E,  classification:EntityClassification): RegE[E] = {
    ENTITIES.register(name, () => EntityType.Builder.create(ctor.butFirst(initializer)(_,_), classification).setShouldReceiveVelocityUpdates(false).build(name))
  }

  def regTE[T <: TileEntity, B <: Block](name: String, ctor: Supplier[T], validBlocks: RegistryObject[B]*): RegTE[T] = {
    TILE_ENTITIES.register(name,
      () => {
        TileEntityType.Builder.create[T](ctor, validBlocks.map(_.get): _*)
      }.build(null)
    )
  }


  case class Damageable(maxDamage: Int, defaultMaxDamage: Int)
  case class Stackable(maxStackSize: Int)

  def regSimpleItem(name: String, creativeGroup: Option[ItemGroup] = None, food: Option[Food] = None,
                    stackOrDamage: Either[Damageable, Stackable] = Right(Stackable(64)),
                    fireImmune: Boolean = false, noRepair: Boolean = false,
                    toolTypes: Set[(ToolType, Int)] = Set.empty, rarity: Rarity = Rarity.COMMON,
                    ister: Option[Supplier[Callable[ItemStackTileEntityRenderer]]] = None): RegistryObject[Item] = {

    def applyProps: Item.Properties => Item.Properties = {
      creativeGroup.fold[Item.Properties => Item.Properties](_.group(AnimaCreativeGroup))(g => it => it.group(g)) andThen
        food.foldId[Item.Properties](_.food(_)) andThen
        ister.foldId[Item.Properties](_.setISTER(_)) andThen
        (firstip => toolTypes.foldLeft(firstip) { case (ip, (typ, level)) => ip.addToolType(typ, level) }) andThen
        stackOrDamage.fold(m => { i: Item.Properties => i.maxDamage(m.maxDamage).defaultMaxDamage(m.defaultMaxDamage) }, s => { i: Item.Properties => i.maxStackSize(s.maxStackSize) }) andThen
        (if (fireImmune) _.isImmuneToFire else identity[Item.Properties]) andThen
        (if (noRepair) _.setNoRepair else identity[Item.Properties]) andThen
        (_.rarity(rarity))
    }

    ITEMS.register(name, () => new Item(applyProps(new Item.Properties)))
  }

  def regExtendedItem[I <: Item](name: String, supp: Supplier[I]): RegO[I] = {
    ITEMS.register(name, supp)
  }

  def regBlock[B <: Block](name: String, supp: Supplier[B]): RegO[B] = {
    BLOCKS.register(name, supp)
  }

  def regContainer[C <: Container](name: String, fac: IContainerFactory[C]): RegCT[C] = {
    CONTAINERS.register(name, () => IForgeContainerType.create(fac))
  }


}
