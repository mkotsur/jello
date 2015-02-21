package com.xebialabs.jello.domain

import com.typesafe.config.ConfigFactory
import com.xebialabs.jello.TestSugar
import com.xebialabs.jello.domain.Jira.Ticket
import com.xebialabs.jello.domain.Trello.{Board, Column}
import com.xebialabs.restito.builder.stub.StubHttp.whenHttp
import com.xebialabs.restito.builder.verify.VerifyHttp.verifyHttp
import com.xebialabs.restito.semantics.Action._
import com.xebialabs.restito.semantics.Condition
import com.xebialabs.restito.semantics.Condition._
import com.xebialabs.restito.server.StubServer

import scala.language.postfixOps

class TrelloTest extends TestSugar {

  private val server: StubServer = new StubServer()

  override protected def beforeAll(): Unit = {
    server.run()
    System.setProperty("trello.apiUri", s"http://localhost:${server.getPort}")
    ConfigFactory.invalidateCaches()
  }

  override protected def afterAll(): Unit = {
    server.stop()
  }


  describe("trello client") {

    it("should create a board with lists and cards at column 0") {

      whenHttp(server)
        .`match`(post("/boards"), parameter("key", "k"), parameter("token", "t"))
        .then(resourceContent("boards.post.json"))

      whenHttp(server)
        .`match`(post("/boards/b1/lists"))
        .then(resourceContent("boards.lists.post.json"))


      new Trello().createBoard().futureValue

      verifyHttp(server).once(post("/boards"))


      val newListCondition = (name: String, pos: Int) => Condition.composite(
        post("/boards/b1/lists"),
        withPostBodyContaining(s""""name": "$name"""")
      )

      verifyHttp(server)
        .once(newListCondition("0", 10)).then()
        .once(newListCondition("11", 20)).then()
        .once(newListCondition("23", 30)).then()
        .once(newListCondition("24", 40)).then()
        .once(newListCondition("25", 50)).then()
        .once(newListCondition("26", 60))
    }

    it("should put tickets") {

      whenHttp(server)
        .`match`(post("/cards"))
        .then(contentType("application/json"), stringContent("""{"id": "c1"}"""))

      val board = Board("b1", "http://url.short", Seq(Column("0", "c0"), Column("1", "c1"), Column("42", "c42")))

      whenReady(board.putTickets(Seq(Ticket("t1", "Ticket 1"), Ticket("t2", "Ticket 2")))) { r =>
        verifyHttp(server)
          .once(
            post("/cards"),
            withPostBodyContaining(s"""Ticket 1""""),
            withPostBodyContaining(s""""idList": "0"""")
          ).then().once(
            post("/cards"),
            withPostBodyContaining(s"""Ticket 2""""),
            withPostBodyContaining(s""""idList": "0"""")
          )

      }

    }

    it("should archive board") {
      whenHttp(server)
        .`match`(put("/boards/b2/closed"))
        .then(resourceContent("boards.closed.put.json"))

      whenReady(new Trello().archiveBoard("b2")) { r =>
        verifyHttp(server).once(
          put("/boards/b2/closed"),
          withPostBodyContaining(s""""value": true""")
        )
      }
    }

    it("should return estimated tickets") {

      val trello = new Trello

      whenHttp(server)
        .`match`(get("/boards/b1/lists"), parameter("cards", "open"))
        .then(resourceContent("boards.b1.lists.json"))

      val tickets = trello.getTickets("b1").futureValue

      tickets should have length 3
      tickets should contain(Ticket("T-1", "Do everything good", Some(0)))
      tickets should contain(Ticket("T-2", "Fix all bugs", Some(2)))
      tickets should contain(Ticket("T-3", "Do not create new bugs", Some(2)))

    }
  }

}
