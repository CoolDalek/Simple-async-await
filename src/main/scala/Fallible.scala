trait Fallible[F[_]]:

  def failed[T](exc: Throwable): F[T]

  def succeeded[T](value: T): F[T]

object Fallible:

  type Either[T] = scala.Either[Throwable, T]

  given Fallible[Either] with

    inline def failed[T](exc: Throwable): Either[T] = Left(exc)

    inline def succeeded[T](value: T): Either[T] = Right(value)

  end given

  given Fallible[Id] with

    inline def failed[T](exc: Throwable): Nothing = throw exc

    inline def succeeded[T](value: T): T = value

  end given

  inline def apply[F[_]](using fallible: Fallible[F]): Fallible[F] = fallible
  
end Fallible