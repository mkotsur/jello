package com.xebialabs.jello.domain

import com.xebialabs.jello.domain.Jira.Ticket
import com.xebialabs.jello.domain.json.JiraProtocol
import com.xebialabs.jello.http.RequestExecutor
import spray.http.HttpResponse
import spray.httpx.SprayJsonSupport._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Jira {
  case class Ticket(id: String, title: String, estimation: Option[Int] = None)
}

class Jira extends RequestExecutor with JiraProtocol {

  def getTicket(id: String): Future[Ticket] = runRequest[TicketResp](GetTicketReq(id))

  def updateEstimation(t: Ticket): Future[Ticket] = runRequest[HttpResponse](EstimateUpdateReq(t)).map(r => t)

  def getRapidBoardTickets(rapidBoardId: Int, startWith: String, endWith: String): Future[Seq[Ticket]] = {
    val f: Future[Seq[Ticket]] = runRequest[RapidBoardResp](GetRapidBoardReq(rapidBoardId))
    f.map {
      tickets =>
        tickets.slice(tickets.indexWhere(_.id == startWith), tickets.indexWhere(_.id == endWith) + 1)
    }
  }

}
