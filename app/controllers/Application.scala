package controllers

import play.api._
import play.api.mvc._

import models._
import utils._

object Application extends Controller with Secured {
    
    def index = MayAuthenticateTokenized { auth => token => implicit req =>
        Ok(views.html.index(token))
    } { token => implicit req =>
        Ok(views.html.anonindex(token))
    }
}