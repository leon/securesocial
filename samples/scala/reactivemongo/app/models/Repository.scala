package models

import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection
import scala.reflect.ClassTag

import play.api.Play.current

abstract class Repository extends Product {
  /** Returns the current instance of the driver. */
  def driver = ReactiveMongoPlugin.driver
  /** Returns the current MongoConnection instance (the connection pool manager). */
  def connection = ReactiveMongoPlugin.connection
  /** Returns the default database (as specified in `application.conf`). */
  def db = ReactiveMongoPlugin.db

  def collection: JSONCollection = db.collection(collectionName)

  def collectionName: String = {
    val n = productPrefix.toLowerCase
    n
  }
}
