package controllers

import play.api._
import play.api.mvc._

import models._
import utils._

object Application extends Controller with Secured {
    
    def index = MayAuthenticate ({ auth => Action { implicit req =>
        Ok(views.html.index(authenticityToken))
    }
    }, { implicit req =>
        Ok(views.html.anonindex(authenticityToken))
    })
}