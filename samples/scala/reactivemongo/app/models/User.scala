package models

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.concurrent.Future
import play.api.libs.Codecs
import reactivemongo.api.indexes.{IndexType, Index}

// Secure Social
import securesocial.core._

// Reactive Mongo imports
import reactivemongo.api._

// Reactive Mongo plugin, including the JSON-specialized collection
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection

import play.api.libs.concurrent.Execution.Implicits.defaultContext

// We need an implementation of Identity to serialize to mongo
case class MongoIdentity(
  identityId: IdentityId,
  email: Option[String],
  firstName: String,
  lastName: String,
  fullName: String,
  avatarUrl: Option[String],
  authMethod: AuthenticationMethod,
  oAuth1Info: Option[OAuth1Info],
  oAuth2Info: Option[OAuth2Info],
  passwordInfo: Option[PasswordInfo]
) extends Identity

object MongoIdentity {

  object Implicits {
    implicit val identityIdFormat = Json.format[IdentityId]
    implicit val authenticationMethodFormat = Json.format[AuthenticationMethod]
    implicit val oAuth1InfoFormat = Json.format[OAuth1Info]
    implicit val oAuth12nfoFormat = Json.format[OAuth2Info]
    implicit val passwordInfoFormat = Json.format[PasswordInfo]
    implicit val identityFormat = Json.format[MongoIdentity]
  }

  import Implicits._

  def fromIdentity(i: Identity): MongoIdentity = {
    new MongoIdentity(
      i.identityId,
      i.email,
      i.firstName,
      i.lastName,
      i.fullName,
      i.avatarUrl,
      i.authMethod,
      i.oAuth1Info,
      i.oAuth2Info,
      i.passwordInfo
    )
  }
}

case class User(
  identities: List[MongoIdentity],
  firstName: String,
  lastName: String,
  email: Option[String]
) extends Identity {

  def fullName: String = s"$firstName $lastName"

  val primaryIdentity = identities.head

  override def identityId: IdentityId = primaryIdentity.identityId
  override def avatarUrl: Option[String] = primaryIdentity.avatarUrl.orElse {
    Some(s"http://www.gravatar.com/avatar/${Codecs.md5(email.getOrElse(identityId.userId).getBytes)}.png")
  }
  override def authMethod: AuthenticationMethod = primaryIdentity.authMethod
  override def oAuth1Info: Option[OAuth1Info] = primaryIdentity.oAuth1Info
  override def oAuth2Info: Option[OAuth2Info] = primaryIdentity.oAuth2Info
  override def passwordInfo: Option[PasswordInfo] = primaryIdentity.passwordInfo
}

object User {

  import MongoIdentity.Implicits._

  implicit val format = Json.format[User]

  def fromIdentity(i: Identity): User = {
    new User(
      identities = List(MongoIdentity.fromIdentity(i)),
      firstName = i.firstName,
      lastName = i.lastName,
      email = i.email
    )
  }
}

case object Users extends Repository {

  import MongoIdentity.Implicits._

  collection.indexesManager.ensure(Index(
    key = Seq("email" -> IndexType.Hashed),
    name = Some("emailIndex"),
    unique = true,
    dropDups = true
  ))

  def all: Future[List[User]] = {
    collection.find(Json.obj()).cursor[User].collect[List]()
  }

  def identityKey(identityId: IdentityId) = Json.obj(
    "identities" -> Json.obj(
      "$elemMatch" -> identityId
    )
  )

  def findByIdentityId(identityId: IdentityId): Future[Option[User]] = {
    collection
      .find(identityKey(identityId))
      .one[User]
  }

  def findByEmailAndProviderId(email: String, providerId: String): Future[Option[User]] = {
    collection
      .find(Json.obj(
        "email" -> email,
        "identities.providerId" -> providerId
      ))
      .one[User]
  }

  def findByEmail(email: String): Future[Option[User]] = {
    collection
      .find(Json.obj("email" -> email))
      .one[User]
  }

  def save(user: User): Future[Identity] = {
    val upsert = findByIdentityId(user.identityId).flatMap {
      case Some(existingUser) => collection.update(identityKey(user.identityId), user)
      case None => collection.insert(user)
    }

    upsert.map(lastError => user)
  }

  def link(current: Identity, to: Identity) {
    findByIdentityId(current.identityId).map { userOpt =>
      userOpt.map { user =>
        if (user.identities.contains(to.identityId)) {
          user
        } else {
          save(user.copy(identities = user.identities :+ MongoIdentity.fromIdentity(to)))
        }
      }
    }
  }
}
