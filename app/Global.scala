import play.api._
import play.api.libs.concurrent._

import models._

object Global extends GlobalSettings {
    override def onStart(app: Application) {
        Logger.info("Application has started")

        val delay = akka.util.Duration(10, java.util.concurrent.TimeUnit.SECONDS)
        val period = akka.util.Duration(30, java.util.concurrent.TimeUnit.MINUTES)

        Akka.system(app).scheduler.schedule(delay, period) {
            val howmany = AccessToken.deleteExpired
            Logger.debug("Removed "+howmany+" expired access tokens.")
        }
    }  
}