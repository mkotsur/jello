package com.xebialabs.jello.domain

import com.xebialabs.jello.conf.{ConfigAware, JelloConfig}
import com.xebialabs.jello.domain.Jira.Ticket
import com.xebialabs.jello.domain.Trello.{Board, Card, Column, TokenPermission}
import com.xebialabs.jello.domain.json.TrelloProtocol
import com.xebialabs.jello.http.RequestExecutor
import com.xebialabs.jello.support._
import com.xebialabs.restito.builder.stub.StubHttp.whenHttp
import com.xebialabs.restito.builder.verify.VerifyHttp.verifyHttp
import com.xebialabs.restito.semantics.Action._
import com.xebialabs.restito.semantics.Condition
import com.xebialabs.restito.semantics.Condition._
import com.xebialabs.restito.server.StubServer
import org.glassfish.grizzly.http.util.HttpStatus
import spray.http.{HttpResponse, StatusCodes}
import spray.httpx.UnsuccessfulResponseException

import scala.language.postfixOps

class TrelloTest extends UnitTestSugar {

  private val server: StubServer = new StubServer().run()

  private val testConfig = JelloConfig().withOverrides(Map("trello.apiUri" -> s"http://localhost:${server.getPort}"))

  private trait TestConfig extends ConfigAware {
    override def config: JelloConfig = testConfig
  }

  private val trello = new Trello() with TestConfig

  override protected def afterAll(): Unit = {
    server.stop()
  }


  describe("trello client") {

    it("should create a board with lists and cards at the first list") {

      whenHttp(server)
        .`match`(post("/boards"), parameter("key", "k"), parameter("token", "t"))
        .then(resourceContent("boards.post.json"))

      whenHttp(server)
        .`match`(post("/boards/b1/lists"))
        .then(resourceContent("boards.lists.post.json"))


      trello.createBoard().futureValue

      verifyHttp(server).once(post("/boards"))


      val newListCondition = (name: String, pos: Int) => Condition.composite(
        post("/boards/b1/lists"),
        withPostBodyContaining(s""""name": "$name"""")
      )

      verifyHttp(server)
        .once(newListCondition("Home", 10)).then()
        .once(newListCondition("0", 20)).then()
        .once(newListCondition("11", 30)).then()
        .once(newListCondition("23", 40)).then()
        .once(newListCondition("24", 50)).then()
        .once(newListCondition("25", 60))
    }

    it("should put tickets") {

      whenHttp(server)
        .`match`(post("/cards"))
        .then(resourceContent("cards.post.json"))

      val board = new Board("b1", "Some board", "http://url.short", Seq(Column("0", "c0"), Column("1", "c1"), Column("42", "c42"))) {
        override def config: JelloConfig = testConfig
      }

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

    it("should close a board") {
      whenHttp(server)
        .`match`(put("/boards/b2/closed"))
        .then(resourceContent("boards.closed.put.json"))

      whenReady(trello.closeBoard("b2")) { r =>
        verifyHttp(server).once(
          put("/boards/b2/closed"),
          withPostBodyContaining(s""""value": true""")
        )
      }
    }

    it("should return estimated tickets") {

      whenHttp(server)
        .`match`(get("/boards/b1/lists"), parameter("cards", "open"))
        .then(resourceContent("boards.b1.lists.json"))

      val columns = (new Board("b1", "Some board", "http://aaa") with TestConfig).getColumns.futureValue

      columns shouldEqual Seq(
        Column("i1", "0", Seq(Card("c1", "T-1 Do everything good"))),
        Column("i2", "1", Seq()),
        Column("i3", "2", Seq(Card("c2", "T-2 Fix all bugs"), Card("c3", "T-3 Do not create new bugs")))
      )

    }

    it("should update label") {
      whenHttp(server)
        .`match`(get("/boards/b1/labels"))
        .then(resourceContent("boards.b1.labels.json"))

      whenHttp(server)
        .`match`(post("/cards/c1/idLabels"))
        .then(status(HttpStatus.NO_CONTENT_204))


      (new Board("b1", "Some board", "http://bbb") with TestConfig).updateLabel(Card("c1", "Card 1")).futureValue

      verifyHttp(server)
        .once(post("/cards/c1/idLabels"), withPostBodyContaining("\"value\": \"l1\""))

    }

    it("should return a failed future when application token is not correct") {

      whenHttp(server)
        .`match`(get("/boards/b1/labels"), parameter("token", "wrong-token"))
        .then(unauthorized(), stringContent("invalid token"))


      new TrelloProtocol with RequestExecutor with ConfigAware { this: ConfigAware =>

        override def config: JelloConfig = testConfig.withOverrides(Map("trello.appToken" -> "wrong-token"))

        val respFuture = runRequest[HttpResponse](GetLabelsReq("b1"))

        whenReady(respFuture.failed) {
          case ex: UnsuccessfulResponseException =>
            ex.response.status shouldBe StatusCodes.Unauthorized
            ex.getMessage should include("invalid token")
          case e => fail(s"Expected different exception here, but got $e.")
        }
      }
    }

    it("should return token information") {
      whenHttp(server)
        .`match`(get("/tokens/t"))
        .then(resourceContent("tokens.t.json"))

      val tokenInfo = trello.getTokenInfo.futureValue
      tokenInfo.identifier shouldBe "Jello"
      tokenInfo.permissions should contain(TokenPermission("*", "Board", read = true, write = true))
    }

    it("should list boards") {
      whenHttp(server)
        .`match`(get("/members/someuser/boards"))
        .then(resourceContent("boards.list.json"))

      val boards = trello.listBoards("someuser").futureValue
      boards should have size 2
      val Seq(b1, b2) = boards

      b1.id shouldBe "4eea4ffc91e31d1746000046"
      b1.name shouldBe "Example Board"

      b2.id shouldBe "4ee7e707e582acdec800051a"
      b2.name shouldBe "Public Board"
    }

    it("should delete a board") {
      whenHttp(server)
        .`match`(delete("/boards/bbbb01/members/someuser"))
        .then(status(HttpStatus.OK_200))

      trello.deleteBoard("bbbb01", "someuser").futureValue

      verifyHttp(server).once(delete("/boards/bbbb01/members/someuser"))
    }

  }

}
