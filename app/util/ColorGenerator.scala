package util

import scala.util.Random

object ColorGenerator {
  def generate: String = colors(Random.nextInt(colors.length))

  val colors = IndexedSeq(
    "#ec6fa3", "#f1a3c3", "#e5c7d1", "#c899c5", "#8584be",
    "#bcc5e3", "#809cc1", "#7191c7", "#245b93", "#018c9e",
    "#008c73", "#31504a", "#fce9b7", "#a8cd6f", "#dde6ae",
    "#fbdd04", "#fccc40", "#fbd296", "#f59a5a", "#d16f48",
    "#cc5151", "#f4a79b", "#d3a47b", "#a07769", "#fbe1cb"
  )
}
