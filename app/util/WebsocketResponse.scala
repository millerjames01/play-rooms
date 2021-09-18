package util

import play.api.libs.json._

object WebsocketResponse {
  def jsResponse(kind: String, info: JsValue): JsValue = JsObject(Map(
    "type"  -> JsString(kind),
    "info" -> info
  ))

  def jsMessage(kind: String, message: String): JsValue =
    jsResponse(kind, JsString(message))

  def userJoinMessage(userAlias: UserAlias): JsValue = JsObject(Map(
      "type" -> JsString("user-join"),
      "id" -> JsString(userAlias.clientId),
      "color" -> JsString(userAlias.color),
      "info" -> JsString(userAlias.clientId + " has joined the chat.")
    ))

  def jsError(message: String): JsValue =
    jsMessage("error", message)
}