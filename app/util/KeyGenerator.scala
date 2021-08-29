package util

import scala.io.Source
import scala.util.Random

object KeyGenerator {
  def generateRoomKey: String = {
    val keys = for (_ <- 1 to 3) yield nouns(Random.nextInt(nouns.length)).capitalize
    keys.mkString
  }

  private lazy val nouns = {
    val nounsSource = Source.fromFile("app/resources/nouns.txt")
    val list = nounsSource.getLines().toIndexedSeq
    nounsSource.close
    list
  }
}
