trait UrlEncodedWriter1[A] {
  def encode(value: A): String
}

object UrlEncodedWriter1 {
  def apply[A](implicit ev: UrlEncodedWriter1[A]): UrlEncodedWriter1[A] = ev

  implicit val int: UrlEncodedWriter1[Int] = new UrlEncodedWriter1[Int] {
    def encode(value: Int): String = value.toString()
  }

  final class UrlEncodedWriter1Ops[A](value: A, encoder: UrlEncodedWriter1[A]) {
    def encode: String = encoder.encode(value)
  }

  object ops {
    // implicit conversion. implicit def with non implicit parameter.
    // -> Allows scala to automatically the non implicit parameter to the result type
    implicit def toUrlEncodedWriterOps[A: UrlEncodedWriter1](value: A): UrlEncodedWriter1Ops[A] =
      new UrlEncodedWriter1Ops(value, UrlEncodedWriter1[A])
  }
}

import UrlEncodedWriter1.UrlEncodedWriter1Ops
import UrlEncodedWriter1.ops.toUrlEncodedWriterOps

UrlEncodedWriter1[Int]

1.encode

// 1.encode

// val foo: UrlEncodedWriter1[Int] = ???
// foo.encode(1)
