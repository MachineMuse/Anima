package net.machinemuse.anima
package util

import com.google.common.base.Preconditions
import com.google.gson.{GsonBuilder, JsonObject}
import com.mojang.datafixers.util.Pair
import com.mojang.serialization.Codec
import net.minecraft.advancements.criterion._
import net.minecraft.block.{Block, BlockState}
import net.minecraft.data.loot.{BlockLootTables, EntityLootTables}
import net.minecraft.data.{BlockModelProvider => _, ItemModelProvider => _, _}
import net.minecraft.entity.player.{PlayerEntity, PlayerInventory}
import net.minecraft.inventory.container.{Container, INamedContainerProvider}
import net.minecraft.item.crafting.{IRecipeSerializer, Ingredient}
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.loot._
import net.minecraft.state._
import net.minecraft.tags.ITag
import net.minecraft.util.text.{ITextComponent, TranslationTextComponent}
import net.minecraft.util.{IItemProvider, ResourceLocation}
import net.minecraftforge.client.model.generators.ModelFile.{ExistingModelFile, UncheckedModelFile}
import net.minecraftforge.client.model.generators.{BlockStateProvider => DatagenBlockStateProvider, _}
import net.minecraftforge.common.crafting.conditions.IConditionBuilder
import net.minecraftforge.common.crafting.{CraftingHelper, NBTIngredient}
import net.minecraftforge.common.data.{GlobalLootModifierProvider, LanguageProvider}
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent

import java.io.IOException
import java.nio.file.Path
import java.util.function._
import scala.collection.mutable
import scala.jdk.CollectionConverters.{IterableHasAsJava, SeqHasAsJava}

import util.VanillaCodecs.ConvenientCodec
import java.{lang, util}

/**
 * Created by MachineMuse on 2/9/2021.
 */
object DatagenHelpers extends Logging {

  def mkCenteredCuboidShape(width: Double, height: Double) = Block.makeCuboidShape(8.0 - (width/2.0), 0.0D, 8.0 - (width/2.0), 8.0 + (width/2.0), height, 8.0 + (width/2.0))

  private class LootTableProviderWithAccumulator(gen: DataGenerator) extends LootTableProvider(gen) {
    private val tables: mutable.HashSet[(Supplier[Consumer[BiConsumer[ResourceLocation, LootTable.Builder]]], LootParameterSet)] = mutable.HashSet.empty
    def putTable(table: Supplier[Consumer[BiConsumer[ResourceLocation, LootTable.Builder]]], params: LootParameterSet) = tables.add((table, params))
    override def getTables = tables.map(tup => Pair.of(tup._1, tup._2)).toList.asJava

    override def validate(registry: util.Map[ResourceLocation, LootTable], tracker: ValidationTracker): Unit = {}
  }

  private var blockLootTableProvider = none[LootTableProviderWithAccumulator]
  private def getLootTableProvider(implicit event: GatherDataEvent) = blockLootTableProvider match {
    case Some(provider) => provider
    case None => new LootTableProviderWithAccumulator(event.getGenerator).andDo{
      provider =>
        blockLootTableProvider = provider.some
        event.getGenerator.addProvider(provider)
    }
  }

  // TODO: for other parameter sets
  class SimplerBlockLootTable extends BlockLootTables {
    private val tables: mutable.HashSet[(Block, java.util.function.Function[Block, LootTable.Builder])] = mutable.HashSet.empty
    def add(block: Block, table: LootTable.Builder) = tables.add((block, _ => table))
    def add(block: Block, table: Block => LootTable.Builder) = tables.add(block, table(_))

    override def addTables(): Unit = {
      tables.foreach { tup =>
        registerLootTable(tup._1, tup._2)
      }
    }

    override def getKnownBlocks: lang.Iterable[Block] = {
      tables.map(_._1).asJava
    }
  }
  def provideArbitrarySerializer[T](name: String)(toSave: (ResourceLocation, T)*)(implicit event: GatherDataEvent, codec: Codec[T]) = {
    event.getGenerator.addProvider(new IDataProvider {
      private val GSON = (new GsonBuilder).setPrettyPrinting().create
      private val generator = event.getGenerator
      override def act(directoryCache: DirectoryCache): Unit = {
        val path = generator.getOutputFolder
        toSave.foreach{ case (location, el) =>
          val fullPath = getPath(path, location)
          try {
            IDataProvider.save(GSON, directoryCache, codec.writeJson(el), fullPath)
          } catch {
            case ioexception: IOException =>
              logger.error(s"Couldn't save $name in $fullPath: $ioexception")
          }
        }

      }
      def getPath(path: Path, id: ResourceLocation) = {
        path.resolve(s"data/${id.getNamespace}/$name/${id.getPath}.json")
      }
      override def getName: String = name
    })
  }

  def provideBlockLootTable(lootTable: BlockLootTables)(implicit event: GatherDataEvent) = {
    getLootTableProvider.putTable(() => lootTable, LootParameterSets.BLOCK)
  }

  def provideEntityLootTable(lootTable: EntityLootTables)(implicit event: GatherDataEvent) = {
    getLootTableProvider.putTable(() => lootTable, LootParameterSets.ENTITY)
  }

  def mkLootModifierProvider(addModifiers: GlobalLootModifierProvider => Unit)(implicit event: GatherDataEvent): Unit = {
    lootModifierAccumulator.enqueueData(event, "global", addModifiers)
  }
  private val lootModifierAccumulator = new DataProviderAccumulator[String, GlobalLootModifierProvider]((event, name, actualize) =>
    new GlobalLootModifierProvider(event.getGenerator, implicitly[MODID]) {
      override def start(): Unit = actualize()
    }
  )

  def mkContainerProvider[T <: Container](name: String, menuctor: (Int, PlayerInventory, PlayerEntity) => T): INamedContainerProvider = {
    new INamedContainerProvider() {
      override def getDisplayName: ITextComponent = new TranslationTextComponent(s"screen.${implicitly[MODID]}.$name")

      override def createMenu(windowId: Int, playerInventory: PlayerInventory, playerEntity: PlayerEntity): T = menuctor(windowId, playerInventory, playerEntity)
    }
  }

  def mkRecipeProvider(reg: Consumer[IFinishedRecipe] => Unit)(implicit event: GatherDataEvent): Unit = {
    if(event.includeServer()) {
      val prov = new RecipeProvider (event.getGenerator) with IConditionBuilder {
        override def registerRecipes (consumer: Consumer[IFinishedRecipe] ): Unit = {
          // do NOT call super() as that will generate all the vanilla recipes!
          reg (consumer)
        }
      }
      event.getGenerator.addProvider (prov)
    }
  }

  def existingModModelFile(path: String)(implicit event: GatherDataEvent): ExistingModelFile = {
    new ExistingModelFile(modLoc(path), event.getExistingFileHelper)
  }

  def existingVanillaModelFile(path: String)(implicit event: GatherDataEvent): ExistingModelFile = {
    new ExistingModelFile(new ResourceLocation(path), event.getExistingFileHelper)
  }
  def uncheckedModelFile(path: String)(implicit event: GatherDataEvent): UncheckedModelFile = {
    new ModelFile.UncheckedModelFile(new ResourceLocation(path))
  }

  def mkAllVariantBlockStates(block: Block)(f: BlockState => Array[ConfiguredModel])(implicit event: GatherDataEvent): Unit = {
    event.getGenerator.addProvider(
      new DatagenBlockStateProvider(event.getGenerator, implicitly[MODID], event.getExistingFileHelper) {
        override def registerStatesAndModels(): Unit = {
          getVariantBuilder(block).forAllStates { state => f(state) }
        }
      }
    )
  }
  def mkAllVariantBlockStatesExcept(block: Block, ignored: Property[_]*)(f: BlockState => Array[ConfiguredModel])(implicit event: GatherDataEvent): Unit = {
    event.getGenerator.addProvider(
      new DatagenBlockStateProvider(event.getGenerator, implicitly[MODID], event.getExistingFileHelper) {
        override def registerStatesAndModels(): Unit = {
          getVariantBuilder(block).forAllStatesExcept({ state => f(state) }, ignored:_*)
        }
      }
    )
  }
  def mkMultipartBlockStates(block: Block)(f: MultiPartBlockStateBuilder => Unit)(implicit event: GatherDataEvent): Unit = {
    event.getGenerator.addProvider(
      new DatagenBlockStateProvider(event.getGenerator, implicitly[MODID], event.getExistingFileHelper) {
        override def registerStatesAndModels(): Unit = {
          f(getMultipartBuilder(block))
        }
      }
    )
  }
  implicit class PartBuilderWorkaround(builder: MultiPartBlockStateBuilder#PartBuilder) {
    // added this because it compiles to Object[] instead of Comparable[] for some reason
    def saferCondition(property: IntegerProperty, values: Int*): MultiPartBlockStateBuilder#PartBuilder = {
      saferCondition[Integer](property, values.map(Int.box):_*)
    }
    def saferCondition(property: BooleanProperty, values: Boolean*): MultiPartBlockStateBuilder#PartBuilder = {
      saferCondition[java.lang.Boolean](property, values.map(Boolean.box):_*)
    }
    def saferCondition[T  <: AnyRef with Comparable[T]](property: Property[T], values: T*): MultiPartBlockStateBuilder#PartBuilder = {
      // TODO: safecheck this somehow
      Preconditions.checkNotNull(property, "Property must not be null".asInstanceOf[Object])
      Preconditions.checkNotNull(values, "Value list must not be null".asInstanceOf[Object])
      Preconditions.checkArgument(values.nonEmpty, "Value list must not be empty".asInstanceOf[Object])
      Preconditions.checkArgument(!builder.conditions.containsKey(property), "Cannot set condition for property \"%s\" more than once", property.getName)
//      Preconditions.checkArgument(builder.canApplyTo(owner), "IProperty %s is not valid for the block %s", prop, owner)
      builder.conditions.putAll(property.asInstanceOf[Property[_]], values.map(_.asInstanceOf[Comparable[_]]).toList.asJava)

      builder
    }
  }

  def mkBlockModel(f: BlockModelProvider => Unit) (implicit event: GatherDataEvent): Unit = {
    event.getGenerator.addProvider(new BlockModelProvider(event.getGenerator, implicitly[MODID], event.getExistingFileHelper) {
      override def registerModels(): Unit = {
        f(this)
      }
    })
  }

  def mkSimpleItemModel(item: Item) (implicit event: GatherDataEvent) : Unit = {
    mkSimpleItemModel(item, s"item/${item.getRegistryName.getPath}")
  }

  def mkSimpleItemModel(item: Item, texturepath: String, parent: String = "item/handheld")(implicit event: GatherDataEvent): Unit = {
    event.getGenerator.addProvider(
      new ItemModelProvider (event.getGenerator, implicitly[MODID], event.getExistingFileHelper) {
        override def registerModels(): Unit = {
          val location = item.getRegistryName
          getBuilder(location.getPath)
            .parent(uncheckedModelFile(parent))
            .texture("layer0", modLoc(texturepath))
        }
      }
    )
  }

  def mkSimpleBlockItemModel(block: Block, file: ModelFile)(implicit event: GatherDataEvent): Unit = {
    event.getGenerator.addProvider(
      new DatagenBlockStateProvider(event.getGenerator, implicitly[MODID], event.getExistingFileHelper) {
        override def registerStatesAndModels(): Unit = {
          simpleBlockItem(block, file)
        }
      }
    )
  }

  def mkSimpleBlockState(block: Block, file: ModelFile)(implicit event: GatherDataEvent): Unit = {
    event.getGenerator.addProvider(
      new DatagenBlockStateProvider(event.getGenerator, implicitly[MODID], event.getExistingFileHelper) {
        override def registerStatesAndModels(): Unit = {
          simpleBlock(block, file)
        }
      }
    )
  }

  def mkOtherBlockStateProvider (f: DatagenBlockStateProvider => Unit) (implicit event: GatherDataEvent): Unit = {
    event.getGenerator.addProvider(
      new DatagenBlockStateProvider(event.getGenerator, implicitly[MODID], event.getExistingFileHelper) {
        override def registerStatesAndModels(): Unit = {
          f(this)
        }
      }
    )
  }

  class DataProviderAccumulator[K, V <: IDataProvider](ctor: (GatherDataEvent, K, () => Unit) => V) {
    private val providers: mutable.HashMap[K, (V, mutable.Queue[V => Unit])] = new mutable.HashMap
    def getOrAddProvider(event: GatherDataEvent, key: K): (V, mutable.Queue[V => Unit]) = {
      if(!providers.contains(key)) {
        val newProvider = ctor(event, key, () => actualize(key))
        event.getGenerator.addProvider(newProvider)
        providers.put(key, (newProvider, new mutable.Queue))
      }
      providers(key)
    }
    def enqueueData(event: GatherDataEvent, key: K, dataToRegister: V => Unit) = {
      getOrAddProvider(event, key)._2.enqueue(dataToRegister)
    }
    def actualize(key: K) = {
      providers.get(key).foreach { case (provider, stuffToDo) =>
        stuffToDo.foreach(_(provider))
      }
    }
  }

  private val tagProviders = new DataProviderAccumulator[ITag.INamedTag[Block], AnimaBlockTagsProvider](
    (event, tag, actualize) =>  new AnimaBlockTagsProvider(event, tag){
                                  override def registerTags() = actualize()
                                }
  )

  class AnimaBlockTagsProvider(event: GatherDataEvent, tag: ITag.INamedTag[Block])
    extends BlockTagsProvider(event.getGenerator, implicitly[MODID], event.getExistingFileHelper)
  {
    def getBuilder = getOrCreateBuilder(tag)
  }

  def mkBlockTagsProvider(tag: ITag.INamedTag[Block])(addTags: TagsProvider.Builder[Block] => Unit)(implicit event: GatherDataEvent): Unit = {
    tagProviders.enqueueData(event, tag, provider => addTags(provider.getBuilder))
  }


  private val languageProviders = new DataProviderAccumulator[String, AnimaLanguageProvider](
    (event, locale, actualize) => new AnimaLanguageProvider(event.getGenerator, locale) {
      override def addTranslations() = actualize()
    }
  )
  abstract class AnimaLanguageProvider(gen: DataGenerator, locale: String)(implicit modid: MODID) extends LanguageProvider(gen, modid, locale) {
    def addScreen(name: String, value: String) = add(s"screen.${modid}.$name", value)
    def addCreativeGroup(value: String) = add(s"itemGroup.${modid}", value)
    def addCreativeGroup(name: String, value: String) = add(s"itemGroup.${modid}.$name", value)
    def addTooltip(name: String, value: String) = add(s"tooltip.${modid}.$name", value)
  }

  def mkLanguageProvider(locale: String)(appendTranslations: AnimaLanguageProvider => Unit)(implicit event: GatherDataEvent): Unit = {
    if(event.includeClient()) {
      languageProviders.enqueueData(event, locale, appendTranslations)
    }
  }

  implicit class FancyShapedRecipeBuilder(builder: ShapedRecipeBuilder) {
    def addKeyAsCriterion(c: Char, i: Item): ShapedRecipeBuilder = {
      builder.key(c, i)
      builder.addCriterion("has_" + c, hasItem(i))
    }
    def addKeyAsCriterion(c: Char, i: ITag[Item]): ShapedRecipeBuilder = {
      builder.key(c, i)
      builder.addCriterion("has_" + c, hasItem(i))
    }
    def buildProperly(consumer: Consumer[IFinishedRecipe], filename: String) = {
      builder.build((a: IFinishedRecipe) => {
        logger.info("Built recipe: " + a.getID)
        consumer.accept(a)
      }, new ResourceLocation(implicitly[MODID], filename))
    }
  }

  def serializeItemStackForRecipe(stack: ItemStack): JsonObject = {
    val json = new JsonObject
    json.addProperty("type", CraftingHelper.getID(NBTIngredient.Serializer.INSTANCE).toString)
    json.addProperty("item", stack.getItem.getRegistryName.toString)
    json.addProperty("count", stack.getCount)
    if (stack.hasTag) {
      json.addProperty("nbt", stack.getTag.toString)
    }
    json
  }

  implicit class FancyShapelessRecipeBuilder(builder: ShapelessRecipeBuilder) {
    def addIngredientAsCriterion(name: String, ingredient: Item) = {
      builder.addIngredient(ingredient)
      builder.addCriterion("has_" + name, hasItem(ingredient))
    }
    def addIngredientAsCriterion(name: String, ingredient: ITag[Item]) = {
      builder.addIngredient(ingredient)
      builder.addCriterion("has_" + name, hasItem(ingredient))
    }
    def addIngredientAsCriterion(name: String, stack: ItemStack) = {
      val ingredient = new NBTIngredient(stack) {}
      builder.addIngredient(ingredient)
      builder.addCriterion("has_" + name, hasItem(stack))
    }
    def buildProperly(consumer: Consumer[IFinishedRecipe], filename: String) = {
      builder.build((a: IFinishedRecipe) => {
        logger.info("Built recipe: " + a.getID)
        consumer.accept(a)
      }, new ResourceLocation(implicitly[MODID], filename))
    }
  }
  object CampfireRecipeBuilder {
    def campfireRecipe(ingredient: ITag[Item], output: Item, exp: Float) = {
      CookingRecipeBuilder.cookingRecipe(Ingredient.fromTag(ingredient), output, exp, 600, IRecipeSerializer.CAMPFIRE_COOKING).addCriterion("has_ingredient", hasItem(ingredient))
    }
    def campfireRecipe(ingredient: Item, output: Item, exp: Float) = {
      CookingRecipeBuilder.cookingRecipe(Ingredient.fromItems(ingredient), output, exp, 600, IRecipeSerializer.CAMPFIRE_COOKING).addCriterion("has_ingredient", hasItem(ingredient))
    }
  }
  implicit class CampfireRecipeBuilder(builder: CookingRecipeBuilder) {
    def buildProperly(consumer: Consumer[IFinishedRecipe], filename: String) = {
      builder.build((a: IFinishedRecipe) => {
        logger.info("Built recipe: " + a.getID)
        consumer.accept(a)
      }, new ResourceLocation(implicitly[MODID], filename))
    }
  }

  def hasItem(item: IItemProvider): InventoryChangeTrigger.Instance = hasItem(ItemPredicate.Builder.create.item(item).build)

  def hasItem(tag: ITag[Item]): InventoryChangeTrigger.Instance = hasItem(ItemPredicate.Builder.create.tag(tag).build)

  def hasItem(predicate: ItemPredicate*) = new InventoryChangeTrigger.Instance(EntityPredicate.AndPredicate.ANY_AND, MinMaxBounds.IntBound.UNBOUNDED, MinMaxBounds.IntBound.UNBOUNDED, MinMaxBounds.IntBound.UNBOUNDED, predicate.toArray)
  def hasItem(itemStack: ItemStack) = new InventoryChangeTrigger.Instance(EntityPredicate.AndPredicate.ANY_AND, MinMaxBounds.IntBound.UNBOUNDED, MinMaxBounds.IntBound.UNBOUNDED, MinMaxBounds.IntBound.UNBOUNDED, Array(ItemPredicate.Builder.create().item(itemStack.getItem).nbt(itemStack.getTag).build()))
}
