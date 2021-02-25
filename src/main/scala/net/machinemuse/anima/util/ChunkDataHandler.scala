package net.machinemuse.anima
package util

import net.minecraft.util.math.{BlockPos, ChunkPos}
import net.minecraft.world.World
import net.minecraft.world.chunk.Chunk

import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import scala.annotation.tailrec

/**
 * Created by MachineMuse on 2/23/2021.
 */
object ChunkDataHandler extends Logging {
  case class WorldData[T](worldRef: WeakReference[World], contents: java.util.concurrent.ConcurrentHashMap[Long, (WeakReference[Chunk], T)]) {

    @inline final def blockPosToChunkLong(x: Int, z: Int): Long = ChunkPos.asLong(x >> 4, z >> 4)

    @inline final def get(pos: Long): Option[(Chunk, T)] = {
      val tup = contents.get(pos)
      if(tup != null) {
        val chunk = tup._1.get
        if(chunk != null) {
          Some((chunk, tup._2))
        } else {
          remove(pos)
        }
      } else {
        none
      }
    }
    @inline final def get(pos: ChunkPos): Option[(Chunk, T)] = get(pos.asLong())
    @inline final def get(pos: BlockPos): Option[(Chunk, T)] = get(blockPosToChunkLong(pos.getX, pos.getZ))
    @inline final def getOrCache(pos: BlockPos, chunk: Chunk, data: T): Option[(Chunk, T)] = {
      if(blockPosToChunkLong(pos.getX, pos.getY) == chunk.getPos.asLong()) {
        Some(chunk -> data)
      } else {
        get(blockPosToChunkLong(pos.getX, pos.getZ))
      }
    }
    @inline final def getCap(pos: Long): Option[T] = get(pos).map(_._2)
    @inline final def getCap(pos: ChunkPos): Option[T] = get(pos).map(_._2)
    @inline final def getCap(pos: BlockPos): Option[T] = get(pos).map(_._2)
    @inline final def update(chunk: Chunk, data: T): Unit = {
      contents.put(chunk.getPos.asLong, (new WeakReference(chunk), data))
    }
    @inline final def remove[X](pos: Long): Option[X] = {
      contents.remove(pos)
      none[X]
    }
    @inline final def iterator: Iterator[(Chunk, T)] = new Iterator[(Chunk, T)] {
      val underlying = contents.entrySet().iterator
      var currNext: (Long, (Chunk, T)) = null
      advance()


      override def hasNext: Boolean = currNext != null

      @tailrec
      def advance(): Unit = if(underlying.hasNext) {
        val nextEntry = underlying.next()
        val nextKey = nextEntry.getKey
        val nextValue = nextEntry.getValue.mapFirst(_.get)
        if(nextValue._1 == null) {
          contents.remove(nextKey)
          advance()
        } else {
          currNext = (nextKey, nextValue)
        }
      } else currNext = null

      override def next(): (Chunk, T) = {
        val curr = currNext._2
        advance()
        curr
      }
    }
  }

  class WorldLoadedChunkDataHandler[T] {
    private val ACTIVE_INTERFACES = new ConcurrentHashMap[WeakReference[World], WorldData[T]]()

    def findWorldKey(world: World) = ACTIVE_INTERFACES.searchKeys(100, ref => if(world == ref.get()) ref else null )

    def putData(world: World, chunk: Chunk, data: T): Unit = {
      findWorldKey(world) match {
        case worldRef: WeakReference[World] =>
          ACTIVE_INTERFACES.get(worldRef).update(chunk, data)
        case null =>
          val chunkRef = new WeakReference(chunk)
          val newData = new ConcurrentHashMap[Long, (WeakReference[Chunk], T)]()
          newData.put(chunk.getPos.asLong, (chunkRef, data))
          val worldRef = new WeakReference(world)
          val worldData = WorldData(worldRef, newData)
          ACTIVE_INTERFACES.put(worldRef, worldData)
      }
    }
    def getData(world: World): Option[WorldData[T]] = {
      val worldKey = findWorldKey(world)
      if(worldKey == null) {
        none
      } else {
        ACTIVE_INTERFACES.get(worldKey).some
      }
    }
  }
}
