package http
import jsonformat.JsDecoder
import eu.timepit.refined
import refined.api.Refined
import refined.string.Url
import scalaz.IList
import scalaz.Monad
import scalaz.syntax.monad._
import scala.concurrent.duration._
import dda.RefreshRequest
import dda.RefreshResponse
import dda.AccessRequest
import dda.AccessResponse
import dda.AuthRequest
import dda.Epoch
import dda.UrlEncodedWriter

trait JsonClient[F[_]] {
  def get[A: JsDecoder](
    uri: String Refined Url,
    headers: IList[(String, String)]
  ): F[A]

  def post[P: UrlEncodedWriter, A: JsDecoder](
    uri: String Refined Url,
    payload: P,
    headers: IList[(String, String)] = IList.empty
  ): F[A]
}

object JsonClient {
  sealed abstract class Error
  final case class ServerError(status: Int) extends Error
  final case class DecodingError(message: String) extends Error
}

final case class CodeToken(token: String, redirect_uri: String Refined Url)

trait UserInteration[F[_]] {
  def start: F[String Refined Url]
  def open(uri: String Refined Url): F[Unit]
  def stop: F[CodeToken]
}
