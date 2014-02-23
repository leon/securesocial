package models

import play.api._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import scala.concurrent.Future
import securesocial.core.providers.Token
import play.modules.reactivemongo.MongoController

// Reactive Mongo imports
import reactivemongo.api._

// Reactive Mongo plugin, including the JSON-specialized collection
import play.modules.reactivemongo.json.collection.JSONCollection

import org.joda.time.DateTime

case object Tokens extends Repository {

  implicit val tokenFormat = Json.format[Token]

  def findByUUID(uuid: String): Future[Option[Token]] = {
    collection
      .find(Json.obj("uuid" -> uuid))
      .one[Token]
  }

  def save(t: Token): Future[Token] = {
    val upsert = findByUUID(t.uuid).map {
      case Some(token) => collection.update(Json.obj("uuid" -> t.uuid), t)
      case None => collection.insert(t)
    }

    upsert.map(lastError => t)
  }

  def remove(uuid: String) {
    collection
      .remove(Json.obj("uuid" -> uuid))
  }

  def removeExpired() {
    collection.remove(Json.obj("expirationTime" -> Json.obj("$lte" -> DateTime.now)))
  }
}