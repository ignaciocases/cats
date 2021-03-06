package cats

import simulacrum._

/**
 * Weaker version of Applicative[F]; has apply but not pure.
 *
 * Must obey the laws defined in [[laws.ApplyLaws]].
 */
trait Apply[F[_]] extends Functor[F] with ApplyArityFunctions[F] { self =>

  /**
   * Given a value and a function in the Apply context, applies the
   * function to the value.
   */
  def apply[A, B](fa: F[A])(f: F[A => B]): F[B]

  /**
   * apply2 is a binary version of apply, defined in terms of apply.
   */
  def apply2[A, B, Z](fa: F[A], fb: F[B])(f: F[(A, B) => Z]): F[Z] =
    apply(fb)(apply(fa)(map(f)(f => (a: A) => (b: B) => f(a, b))))

  /**
   * Applies the pure (binary) function f to the effectful values fa and fb.
   *
   * map2 can be seen as a binary version of [[cats.Functor]]#map.
   */
  def map2[A, B, Z](fa: F[A], fb: F[B])(f: (A, B) => Z): F[Z] =
    apply(fb)(map(fa)(a => (b: B) => f(a, b)))

  /**
   * Two sequentially dependent Applys can be composed.
   *
   * The composition of Applys `F` and `G`, `F[G[x]]`, is also an Apply.
   *
   * val ap = Apply[Option].compose[List]
   * val x = Some(List(1, 2))
   * val y = Some(List(10, 20))
   * ap.map2(x, y)(_ + _) == Some(List(11, 12, 21, 22))
   */
  def compose[G[_]](implicit GG: Apply[G]): Apply[Lambda[X => F[G[X]]]] =
    new CompositeApply[F, G] {
      def F: Apply[F] = self
      def G: Apply[G] = GG
    }
}

object Apply {
  def apply[F[_]](implicit ev: Apply[F]): Apply[F] = ev
}

trait CompositeApply[F[_], G[_]]
  extends Apply[Lambda[X => F[G[X]]]] with Functor.Composite[F, G] {
  def F: Apply[F]
  def G: Apply[G]

  def apply[A, B](fa: F[G[A]])(f: F[G[A => B]]): F[G[B]] =
    F.apply(fa)(F.map(f)(gab => G.apply(_)(gab)))
}
