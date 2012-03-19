package controllers

import play.api._
import play.api.mvc._

import models._
import utils.Security._

object Application extends Controller {
    
    def index = Authenticated ({ auth =>
    	Action {
        	Ok(views.html.index())
    	}
	}, _ => Ok(views.html.index()))
}