package com.xebialabs.jello.domain

import com.typesafe.config.ConfigFactory
import com.xebialabs.jello.domain.Jira.Ticket
import com.xebialabs.jello.support.UnitTestSugar
import com.xebialabs.restito.builder.stub.StubHttp.whenHttp
import com.xebialabs.restito.builder.verify.VerifyHttp.verifyHttp
import com.xebialabs.restito.semantics.Action._
import com.xebialabs.restito.semantics.Condition._
import com.xebialabs.restito.server.StubServer
import org.glassfish.grizzly.http.Method
import org.glassfish.grizzly.http.util.HttpStatus

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class JiraTest extends UnitTestSugar {

  private val server = new StubServer()

  override protected def beforeAll(): Unit = {
    server.run()
    System.setProperty("jira.apiUri", s"http://localhost:${server.getPort}")
    ConfigFactory.invalidateCaches()
  }


  override protected def afterAll(): Unit = {
    server.stop()
  }


  describe("jira client") {

    it("should get a ticket by ID") {

      whenHttp(server)
        .`match`(get("/api/latest/issue/T-1"))
        .then(resourceContent("issue.T-1.json"))

      val ticket = Await.result(new Jira().getTicket("T-1"), 1 second)

      ticket.id should equal("T-1")
      ticket.title should equal("Fix it")

      verifyHttp(server).once(get("/api/latest/issue/T-1"))
    }

    it("should get a range of tickets from RapidBoard") {
      whenHttp(server)
      .`match`(get("/greenhopper/1.0/xboard/plan/backlog/data.json"), parameter("rapidViewId", "13"))
      .then(resourceContent("rapidviews.list.json"))


      val tickets = Await.result(new Jira().getRapidBoardTickets(13, "REL-2132", "REL-1084"), 10 seconds)

      tickets.length should be > 0
      tickets.find(_.id == "REL-2227") shouldBe None

      tickets.head shouldEqual Ticket("REL-2132", "[TIMEBOXED] Technical dept analysis: Triggers")

      tickets.indexWhere(_.id == "REL-1084") shouldBe tickets.length - 1
      tickets.find(_.id == "REL-2262") shouldBe None
    }

    it("should update ticket") {
      whenHttp(server)
      .`match`(startsWithUri("/api/2/issue/T-"), method(Method.PUT))
      .then(status(HttpStatus.NO_CONTENT_204))

      val jira = new Jira()
      jira.updateEstimation(Ticket("T-1", "Some ticket with estimation", Some(4))).futureValue
      jira.updateEstimation(Ticket("T-2", "Some ticket without", None)).futureValue

      verifyHttp(server)
        .once(put("/api/2/issue/T-1"), withPostBodyContaining("\"customfield_10012\": 4"))
        .then()
        .once(put("/api/2/issue/T-2"), withPostBodyContaining("\"customfield_10012\": null"))
    }

  }


}
