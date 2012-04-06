package controllers

import play.api._
import play.api.mvc._

import models._
import utils._

object Application extends Controller with Secured {
    
    def index = MayAuthenticate ({ auth => Action {
        Ok(views.html.index())
    }
    }, { _ =>
        Ok(views.html.anonindex())
    })
}