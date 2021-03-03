package net.machinemuse.anima
package bowl

import com.mojang.serialization.Codec
import net.minecraft.block.Block
import net.minecraft.fluid.{Fluid, Fluids}
import net.minecraft.item.ItemStack
import net.minecraft.util.text._

import scala.annotation.nowarn

import util.GenCodecsByName._
import util.Logging
import util.VanillaCodecs._
/**
 * Created by MachineMuse on 3/2/2021.
 */
object BowlContents extends Logging {
  sealed trait BowlContents extends CodecByName {
    def getColour: Int
    def getDisplayName: IFormattableTextComponent
    def isEmpty: Boolean
  }
  case object NoContents extends BowlContents {
    override def getColour = 0x00000000
    override def getDisplayName: IFormattableTextComponent = new StringTextComponent("(nothing)")
    override def isEmpty: Boolean = true
  }
  case class FluidContents(fluid: Fluid) extends BowlContents {
    override def getColour = fluid.getDefaultState.getBlockState.getBlock.getMaterialColor.colorValue
    override def getDisplayName: IFormattableTextComponent = new TranslationTextComponent(fluid.getAttributes.getTranslationKey)
    override def isEmpty: Boolean = fluid == Fluids.EMPTY
  }
  case class BlockContents(block: Block) extends BowlContents {
    override def getColour = block.getMaterialColor.colorValue
    override def getDisplayName: IFormattableTextComponent = block.getTranslatedName
    override def isEmpty: Boolean = !block.getDefaultState.isAir : @nowarn
  }
  case class ItemContents(itemStack: ItemStack) extends BowlContents {
    override def getColour = 0xFFFFFFFF
    override def getDisplayName: IFormattableTextComponent = itemStack.getDisplayName.asInstanceOf[IFormattableTextComponent]
    override def isEmpty: Boolean = itemStack.isEmpty
  }

  val CONTENTS_CODEC = implicitly[Codec[BowlContents]]


  def mkBowlWith(contents: BowlContents): ItemStack = setContents(new ItemStack(BowlWithContents.BOWL_WITH_CONTENTS.get), contents)
  def mkBowlWith(fluid: Fluid): ItemStack = mkBowlWith(FluidContents(fluid))
  def mkBowlWith(block: Block): ItemStack = mkBowlWith(BlockContents(block))
  def mkBowlWith(itemStack: ItemStack): ItemStack = mkBowlWith(ItemContents(itemStack))

  def getContents(stack: ItemStack): BowlContents = {
    for {
      tag <- Option(stack.getTag)
      contents <- CONTENTS_CODEC.parseINBT(tag.getCompound("contents"))
    } yield contents
  }.getOrElse(NoContents)

  def setContents(stack: ItemStack, contents: BowlContents): ItemStack =
    stack.andDo(_.getOrCreateTag().put("contents", CONTENTS_CODEC.writeINBT(contents)))

}
