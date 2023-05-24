package dda

import eu.timepit.refined
import refined.api.Refined
import refined.string.Url

import refined.numeric.Positive
import refined.collection.NonEmpty

import refined.auto._

import jsonformat._
import JsDecoder.ops._

import java.net.URLEncoder

import simulacrum.typeclass
import scalaz.IList

// for intercalate
import scalaz.std.string._
import scalaz.syntax.foldable._

final case class AuthRequest(
  redirect_uri: String Refined Url,
  scope: String,
  client_id: String,
  prompt: String = "consent",
  response_type: String = "code",
  access_type: String = "offline"
)

final case class AccessRequest(
  code: String,
  redirect_uri: String Refined Url,
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

final case class UrlQuery(params: List[(String, String)]) {
  def forUrl(url: String Refined Url): String Refined Url =
    if (params.isEmpty) url
    else {
      // ?foo=bar&baz=bax
      val queryString = params.map { case (k, v) => s"$k=$v" }.mkString("&")
      // will always construct a valid url
      refined.refineV[Url](s"${url.value}?$queryString}").toOption.get
    }
}

@typeclass trait UrlQueryWriter[A]{
  def toUrlQuery(a: A): UrlQuery
}

@typeclass trait UrlEncodedWriter[A]{
  def toUrlEncoded(a: A): String Refined Url
}

import UrlEncodedWriter.ops._

object AccessResponse {
  implicit val json: JsDecoder[AccessResponse] = JsDecoder.obj(0) {j =>
    for {
      acc <- j.getAs[String]("access_token")
      tpe <- j.getAs[String]("token_type")
      exp <- j.getAs[Long]("expires_in")
      ref <- j.getAs[String]("refresh_token")
    } yield AccessResponse(acc, tpe, exp, ref)
  }
}

object RefreshResponse {
  implicit val json: JsDecoder[RefreshResponse] = JsDecoder.obj(0) { j =>
    for {
      acc <- j.getAs[String]("access_token")
      tpe <- j.getAs[String]("token_type")
      exp <- j.getAs[Long]("expires_in")
    } yield RefreshResponse(acc, tpe, exp)
  }
}

object UrlEncodedWriter {
  // implicit val encoder: UrlEncodedWriter[String Refined UrlEncoded] = identity
  implicit val string: UrlEncodedWriter[String] =
    (s => Refined.unsafeApply(URLEncoder.encode(s, "UTF-8")))
  implicit val url: UrlEncodedWriter[String Refined Url] = 
    (s => s.value.toUrlEncoded)
  implicit val long: UrlEncodedWriter[Long] =
    (s => Refined.unsafeApply(s.toString))
  implicit def ilist[K: UrlEncodedWriter, V: UrlEncodedWriter]: UrlEncodedWriter[IList[(K, V)]] = { m =>
    val raw = m.map {
      case (k, v) => k.toUrlEncoded.value + "=" + v.toUrlEncoded.value
    }.intercalate("&")
    Refined.unsafeApply(raw)
  }
}

object AuthRequest {
  implicit val query: UrlQueryWriter[AuthRequest] = { a => 
    UrlQuery(List(
      ("redirect_uri" -> a.redirect_uri.value),
      ("scope" -> a.scope),
      ("client_id" -> a.client_id),
      ("prompt" -> a.prompt),
      ("response_type" -> a.response_type),
      ("access_type" -> a.access_type)
    ))
  }
}

object AccessRequest {
  implicit val encoded: UrlEncodedWriter[AccessRequest] = { a =>
    IList(
      "code" -> a.code.toUrlEncoded,
      "redirect_uri" -> a.redirect_uri.toUrlEncoded,
      "client_id" -> a.client_id.toUrlEncoded,
      "client_secret" -> a.client_secret.toUrlEncoded,
      "scope" -> a.scope.toUrlEncoded,
      "grant_type" -> a.grant_type.toUrlEncoded
    ).toUrlEncoded
  }
}

object RefreshRequest {
  implicit val encoded: UrlEncodedWriter[RefreshRequest] = { r =>
    IList(
      "client_secret" -> r.client_secret.toUrlEncoded,
      "refresh_token" -> r.refresh_token.toUrlEncoded,
      "client_id" -> r.client_id.toUrlEncoded,
      "grant_type" -> r.grant_type.toUrlEncoded
    ).toUrlEncoded
  }
}
