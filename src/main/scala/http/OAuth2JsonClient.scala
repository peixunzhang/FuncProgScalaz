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


trait LocalClock[F[_]] {
  def now: F[Epoch]
}

final case class ServerConfig(
  auth: String Refined Url,
  access: String Refined Url,
  refresh: String Refined Url,
  scope: String,
  clientId: String,
  clientSecret: String
)

final case class RefreshToken(token: String)
final case class BearerToken(token: String, expires: Epoch)

class OAuth2Client[F[_]: Monad](
  config: ServerConfig
)(
  user: UserInteration[F],
  client: JsonClient[F],
  clock: LocalClock[F]
) {
  def authenticate: F[CodeToken] =
    for {
      callback <- user.start
      params = AuthRequest(callback, config.scope, config.clientId)
      _ <- user.open(params.toUrlQuery.forUrl(config.auth))
      code <- user.stop
    } yield code
  
  def access(code: CodeToken): F[(RefreshToken, BearerToken)] =
    for {
      request <- AccessRequest(code.token,
                               code.redirect_uri,
                               config.clientId,
                               config.clientSecret).pure[F]
      msg <- client.post[AccessRequest, AccessResponse](
        config.access, request)
      time <- clock.now
      expires = time + msg.expires_in.seconds
      refresh = RefreshToken(msg.refresh_token)
      bearer = BearerToken(msg.access_token, expires)
    } yield (refresh, bearer)
  
  def bearer(refresh: RefreshToken): F[BearerToken] = 
    for {
      request <- RefreshRequest(config.clientSecret,
                                refresh.token,
                                config.clientId).pure[F]
      msg <- client.post[RefreshRequest, RefreshResponse](
        config.refresh, request)
      time <- clock.now
      expires = time + msg.expires_in.seconds
      bearer = BearerToken(msg.access_token, expires)
    } yield bearer
}

trait ConfigReader[F[_]] {
  def token: F[RefreshToken]
}
