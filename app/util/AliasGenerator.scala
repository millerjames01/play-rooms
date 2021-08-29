package util

import scala.io.Source
import scala.util.Random

object AliasGenerator {
  def generate =
    adjectives(Random.nextInt(adjectives.length)).capitalize + animals(Random.nextInt(animals.length)).capitalize

  private lazy val adjectives = {
    val adjSource = Source.fromFile("app/resources/adjectives.txt")
    val list = adjSource.getLines().toIndexedSeq
    adjSource.close
    list
  }

  private lazy val animals = {
    val animalSource = Source.fromFile("app/resources/animals.txt")
    val list = animalSource.getLines().toIndexedSeq
    animalSource.close
    list
  }
}
