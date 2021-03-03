package net.machinemuse.anima
package bowl

import net.minecraft.block.BlockState
import net.minecraft.nbt.CompoundNBT
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent

import bowl.BowlContents.{BowlContents, NoContents}
import bowl.BowlInWorldTileEntity.getType
import registration.RegistryHelpers.regTE
import util.Logging
import util.VanillaCodecs.ConvenientCodec

/**
 * Created by MachineMuse on 2/26/2021.
 */
object BowlInWorldTileEntity extends Logging {
  @SubscribeEvent def onConstructMod(e: FMLConstructModEvent) = {}
  val TYPE = regTE[BowlInWorldTileEntity]("bowl_in_world", () => new BowlInWorldTileEntity, () => BowlInWorld.BLOCK.get)
  def getType = TYPE.get


}

@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
class BowlInWorldTileEntity extends TileEntity(getType) {
  var contents: BowlContents = NoContents

  override def read(state : BlockState, nbt : CompoundNBT): Unit = {
    super.read(state, nbt)
    BowlContents.CONTENTS_CODEC.parseINBT(nbt.get("contents")) match {
      case Some(value) => contents = value
      case None =>
    }
  }

  override def write(nbt : CompoundNBT): CompoundNBT = {
    super.write(nbt)
    nbt.put("contents", BowlContents.CONTENTS_CODEC.writeINBT(contents))
    nbt
  }

  override def getUpdateTag: CompoundNBT = write(new CompoundNBT())

  override def handleUpdateTag(state: BlockState, tag: CompoundNBT): Unit = {
    super.handleUpdateTag(state, tag)
  }
}
