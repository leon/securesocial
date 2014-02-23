package controllers

import play.api._
import play.api.mvc._
import models._
import play.api.libs.json.Json
import securesocial.core._
import json._
import scala.concurrent.ExecutionContext.Implicits.global

object Application extends Controller with SecureSocial {

  def index = Action.async {
    Users
      .all
      .map { users =>
        Ok(Json.toJson(users))
      }
  }

  def secured = SecuredAction { request =>
    Ok(Json.toJson(request.user))
  }

}