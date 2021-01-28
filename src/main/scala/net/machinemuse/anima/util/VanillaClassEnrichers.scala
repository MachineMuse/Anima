package net.machinemuse.anima.util

import net.machinemuse.anima.Anima
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType
import net.minecraft.entity.player.{PlayerEntity, PlayerInventory}
import net.minecraft.inventory.container.{Container, INamedContainerProvider}
import net.minecraft.item.ItemStack
import net.minecraft.util.Hand
import net.minecraft.util.text.{ITextComponent, TranslationTextComponent}
import org.apache.logging.log4j.LogManager

/**
 * Created by MachineMuse on 1/27/2021.
 */
object VanillaClassEnrichers {
  private val LOGGER = LogManager.getLogger

  implicit class RichTransformType(transformType: TransformType) {
    def isLeftHand = transformType == TransformType.FIRST_PERSON_LEFT_HAND || transformType == TransformType.FIRST_PERSON_LEFT_HAND
  }

  implicit class RichPlayerEntity(player: PlayerEntity) {
    def itemInHand: Hand => ItemStack = {
      case Hand.MAIN_HAND => player.inventory.mainInventory.get(player.inventory.currentItem)
      case Hand.OFF_HAND => player.inventory.offHandInventory.get(0)
    }
  }

  def mkContainerProvider[T <: Container](name: String, menuctor: (Int, PlayerInventory, PlayerEntity) => T): INamedContainerProvider = {
    new INamedContainerProvider() {
      override def getDisplayName: ITextComponent = new TranslationTextComponent(s"screen.${Anima.MODID}.$name")

      override def createMenu(windowId: Int, playerInventory: PlayerInventory, playerEntity: PlayerEntity): T = menuctor(windowId, playerInventory, playerEntity)
    }
  }
}
