package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {
    
    import models._
    
    def index = Action {
        Ok(views.html.index(File.all))
    }
  
}