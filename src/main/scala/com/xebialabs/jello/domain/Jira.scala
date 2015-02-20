package com.xebialabs.jello.domain

import com.xebialabs.jello.domain.Jira.Ticket
import com.xebialabs.jello.domain.json.JiraProtocol
import com.xebialabs.jello.http.RequestExecutor
import spray.httpx.SprayJsonSupport._
import scala.concurrent.Future

object Jira {
  case class Ticket(id: String, title: String, estimation: Option[Int] = None)
}

class Jira extends RequestExecutor with JiraProtocol {

  def getTicket(id: String): Future[Ticket] = runRequest[TicketResp](GetTicketReq(id))

  def putTicket(t: Ticket): Future[Unit] = ???

  def getRapidBoardTickets(rapidBoardId: Int, startWith: String, endWith: String): Future[Seq[]] = {

  }

}
