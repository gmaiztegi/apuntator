import play.api._
import play.api.libs.concurrent._

import akka.util._

import java.util.concurrent.{TimeUnit}

import models._

object Global extends GlobalSettings {
    override def onStart(app: Application) {
        Logger.info("Application has started")

        val delay = Duration(10, TimeUnit.SECONDS)
        val period = Duration(30, TimeUnit.MINUTES)

        Akka.system(app).scheduler.schedule(delay, period) {
            val howmany = AccessToken.deleteExpired
            Logger.debug("Removed "+howmany+" expired access tokens.")
        }
    }  
}