package com.xebialabs.jello.domain

import com.typesafe.config.ConfigFactory
import com.xebialabs.restito.builder.stub.StubHttp.whenHttp
import com.xebialabs.restito.builder.verify.VerifyHttp.verifyHttp
import com.xebialabs.restito.semantics.Action._
import com.xebialabs.restito.semantics.Condition._
import com.xebialabs.restito.server.StubServer
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class JiraTest extends FunSpec with BeforeAndAfterAll with Matchers {

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
        .`match`(get("/issue/T-1"))
        .then(resourceContent("issue.T-1.json"))

      val ticket = Await.result(new Jira().getTicket("T-1"), 1 second)

      ticket.id should equal("T-1")
      ticket.title should equal("Fix it")

      verifyHttp(server).once(get("/issue/T-1"))
    }

  }


}
