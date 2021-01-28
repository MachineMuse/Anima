package net.machinemuse.anima.util

/**
 * Created by MachineMuse on 1/22/2021.
 */
object OptionCast {
  def apply[T: Manifest](x: Any): Option[T] = {
    if (implicitly[Manifest[T]].runtimeClass.isInstance(x)) {
      Some(x.asInstanceOf[T])
    } else {
      None
    }
  }

  implicit class Optionally[I](x: I) {
    def optionallyAs[O: Manifest]: Option[O] =
      if (implicitly[Manifest[O]].runtimeClass.isInstance(x)) {
        Some(x.asInstanceOf[O])
      } else {
        None
      }

    def optionallyAsList[O: Manifest]: List[O] =
      if (implicitly[Manifest[O]].runtimeClass.isInstance(x)) {
        List(x.asInstanceOf[O])
      } else {
        List.empty[O]
      }

    def optionallyDoAs[O: Manifest](f: O => ()): Unit =
      if (implicitly[Manifest[O]].runtimeClass.isInstance(x)) {
        f(x.asInstanceOf[O])
      } else {
        ()
      }
  }
}
