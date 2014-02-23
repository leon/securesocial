package services

import play.api._
import models._
import securesocial.core._
import securesocial.core.providers.Token

import scala.concurrent._
import scala.concurrent.duration._
import play.api.Play.current

/**
 * Blocking UserService
 * Until secure social exposes a non blocking api there isn't much we can do
 * @param application
 */
class UserService(application: Application) extends UserServicePlugin(application) {

  val timeout = 5.seconds

  def find(id: IdentityId): Option[Identity] = Await.result(Users.findByIdentityId(id), timeout)
  def findByEmailAndProvider(email: String, providerId: String): Option[Identity] = Await.result(Users.findByEmailAndProviderId(email, providerId), timeout)
  def save(identity: Identity): Identity = Await.result(Users.save(User.fromIdentity(identity)), timeout)
  def link(current: Identity, to: Identity) = Users.link(current, to)
  def save(token: Token): Unit = Tokens.save(token)
  def findToken(uuid: String): Option[Token] = Await.result(Tokens.findByUUID(uuid), timeout)
  def deleteToken(uuid: String): Unit = Tokens.remove(uuid)
  def deleteExpiredTokens(): Unit = Tokens.removeExpired()
}
