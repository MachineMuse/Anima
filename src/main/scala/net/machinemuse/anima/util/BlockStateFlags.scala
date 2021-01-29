package net.machinemuse.anima
package util

/**
 * Created by MachineMuse on 1/26/2021.
 */
object BlockStateFlags {

  // Flags are as follows:
  // 1 will cause a block update.
  // 2 will send the change to clients.
  // 4 will prevent the block from being re-rendered.
  // 8 will force any re-renders to run on the main thread instead
  // 16 will prevent neighbor reactions (e.g. fences connecting, observers pulsing).
  // 32 will prevent neighbor reactions from spawning drops.
  // 64 will signify the block is being moved.
  // Flags can be OR-ed

  val BLOCK_UPDATE = 1
  val SEND_TO_CLIENTS = 2
  val DONT_RE_RENDER = 4
  val RERENDER_ON_MAIN = 8
  val PREVENT_NEIGHBOUR_REACTIONS = 16
  val PREVENT_NEIGHBOURS_SPAWNING_DROPS = 32
  val BEING_MOVED = 64

  val STANDARD_CLIENT_UPDATE = BLOCK_UPDATE | SEND_TO_CLIENTS | RERENDER_ON_MAIN
}
