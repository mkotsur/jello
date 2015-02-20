package com.xebialabs.jello.app


object TrelloApp {
  val name = "Jello"
  def apply(): TrelloApp = TrelloApp("9a25795c1381fcceed2ad5a2a2107234", "57c8a483b823e416f5d25a2dab456f12", "ebb4381bdfa6f6e747f54f5fb392aaa2ea6d4245caca17dbc667939cf6b8aa3b")
}


// RW Token: 302a6b51ed578267a15f3fab8ebd155df956b5ed85aa4fb678ab8aefdaaa160e

case class TrelloApp(verCode: String, appKey: String, secret: String) {

  def boardsUrl = s"https://trello.com/1/members/my/boards?key=$appKey&token=$verCode"

  def newTokenUrl = s"https://trello.com/1/authorize?key=$appKey&name=${TrelloApp.name}&expiration=30days&response_type=token"

}