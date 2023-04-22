import eu.timepit.refined
import refined.api.Refined

import refined.numeric.Positive
import refined.collection.NonEmpty

import refined.auto._g

import jsonformat._
import JsDecoder.ops._

import java.net.URLEncoder

final case class AuthRequest(
  redirect_uri: String Refined Uri,
  scope: String,
  client_id: String,
  prompt: String = "consent",
  response_type: String = "code",
  access_type: String = "offline"
)

final case class AccessRequest(
  code: String,
  redirect_uri: String Refined Uri,
  client_id: String,
  client_secret: String,
  scope: String = "",
  grant_type: String = "authorization_code"
)

final case class AccessResponse(
  access_token: String,
  token_type: String,
  expires_in: Long,
  refresh_token: String
)

final case class RefreshRequest(
  client_secret: String,
  refresh_token: String,
  client_id: String,
  grant_type: String = "refresh_token"
)

final case class RefreshResponse(
  access_token: String,
  token_type: String,
  expires_in: Long
)

final case class UrlQuery(params: List[(String, String)])

@typeclass trait UrlQueryWriter[A]{
  def toUrlQuery(a: A): UrlQuery
}

@typeclass trait UrlEncodedWriter[A]{
  def toUrlEncoder(a: A): String Refined toUrlEncoded
}

object AccessResponse {
  implicit val json: JsDecoder[AccessResponse] = j =>
    for {
      acc <- j.getAs[String]("access_token")
      tpe <- j.getAs[String]("token_type")
      exp <- j.getAs[Long]("expires_in")
      ref <- j.getAs[String]("refresh_token")
    } yield AccessResponse(acc, tpe, exp, ref)
}

object RefreshResponse {
  implicit val json: JsDecoder[RefreshResponse] = j =>
    for {
      acc <- j.getAs[String]("access_token")
      tpe <- j.getAs[String]("token_type")
      exp <- j.getAs[Long]("expires_in")
    } yield RefreshResponse(acc, tpe, exp)
}

object UrlEncodedWriter {
  implicit val encoder: UrlEncodedWriter[String Refined UrlEncoded] = identity
  implicit val string: UrlEncodedWriter[String] =
    (s => Refined.unsafeApply(URLEncoder.encode(s, "UTF-8")))
  implicit val urlL: UrlEncodedWriter[String Refined Url] = 
    (s => value.toUrlEncoded)
  implicit val long: UrlEncodedWriter[Long] =
    (s => Refined.unsafeApply(s.toString))
  implicit def ilist[K: UrlEncodedWriter, V: UrlEncodedWriter]: UrlEncodedWriter[IList[(K, V)]] = { m =>
    val raw = m.map {
      case (k, v) => k.toUrlEncoded.value
    }
  }
}
