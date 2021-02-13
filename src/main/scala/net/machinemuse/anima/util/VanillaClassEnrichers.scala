package net.machinemuse.anima
package util

import util.VanillaCodecs.ConvenientCodec

import net.minecraft.block.BlockState
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.state.Property
import net.minecraft.util.{Unit => _, _}
import org.apache.logging.log4j.scala.Logging
import shapeless._

/**
 * Created by MachineMuse on 1/27/2021.
 */
object VanillaClassEnrichers extends Logging {
  implicit class RichTransformType(transformType: TransformType) {
    def isLeftHand = transformType == TransformType.FIRST_PERSON_LEFT_HAND || transformType == TransformType.FIRST_PERSON_LEFT_HAND
  }

  implicit class RichPlayerEntity(player: PlayerEntity) {
    def itemInHand: Hand => ItemStack = {
      case Hand.MAIN_HAND => player.inventory.mainInventory.get(player.inventory.currentItem)
      case Hand.OFF_HAND => player.inventory.offHandInventory.get(0)
    }
  }

  implicit class RichItemStack(stack: ItemStack) {
    private val tagName = "transparency"
    def hasTransparency: Boolean = {
      stack.hasTag && stack.getTag.contains(tagName)
    }
    def getTransparency: Float = {
      if(stack.hasTransparency) {
        stack.getTag.getFloat(tagName)
      } else {
        0.0F
      }
    }
    def setTransparency(f: Float): Unit = {
      stack.getOrCreateTag().putFloat(tagName, f)
    }

    def removeTransparency(): Unit = {
      stack.getOrCreateTag().remove(tagName)
    }
  }

  trait HListHasBlockstateUpdate[L <: HList] {
    def updated(list: L, originalState: BlockState): BlockState
  }
  object HListHasBlockstateUpdate {
    type Aux[L <: HList] = HListHasBlockstateUpdate[L]
  }

  implicit val HNilHasBlockstateUpdate = new HListHasBlockstateUpdate[HNil] {
    override def updated(list: HNil, originalState: BlockState): BlockState = originalState
  }

  implicit def HConsHasBlockstateUpdate[V <: Comparable[V], K <: Property[V], Tail <: HList]
  (implicit tail: HListHasBlockstateUpdate.Aux[Tail]) = new HListHasBlockstateUpdate[(K,V) :: Tail] {
    def updated(list: (K, V) :: Tail, originalState: BlockState): BlockState = {
      val (property, value) = list.head
      val tailupdated = tail.updated(list.tail, originalState)
      tailupdated.`with`(property, value)
    }
  }


//  implicit def optionalOf[T](opt: LazyOptional[T]) = if(opt.isPresent) Some(opt.resolve.get()) else None

  type PropertyValuePair[T <: Comparable[T]] = (Property[T], T)
  implicit class RichBlockState(blockState: BlockState) {
    def updated[T <: Comparable[T]](property: Property[T], value: T) = blockState.`with`(property, value)
    def updated[L <: HList](properties: L)(implicit updater: HListHasBlockstateUpdate[L]) =
      updater.updated(properties, blockState)
  }

}
