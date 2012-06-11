package controllers

import play.api._
import play.api.Play.current
import play.api.mvc._

import controllers._

object JsTests extends Controller {

  def index: Action[AnyContent] = {
    if (Play.isDev || Play.isTest) Action(Ok(views.html.tests()))
    else Default.notFound
  }

  def js(file: String): Action[AnyContent] =  {
    if (Play.isDev || Play.isTest) Assets.at("/public/test", file)
    else Default.notFound
  }
}