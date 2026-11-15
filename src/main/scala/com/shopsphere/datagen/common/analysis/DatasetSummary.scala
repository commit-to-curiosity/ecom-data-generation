
package com.shopsphere.datagen.common.analysis

final case class DatasetSummary(
                                 recordCounts: Map[String, Long],
                                 customerProfile: Option[CustomerProfileSummary] = None
                               ) {
  def totalRecords: Long =
    recordCounts.values.foldLeft(0L) { (total, count) =>
      Math.addExact(total, count)
    }
}
