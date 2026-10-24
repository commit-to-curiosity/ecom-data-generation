package com.shopsphere.datagen.common.output

import com.fasterxml.jackson.databind.ObjectMapper

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, StandardOpenOption}

final class JsonLinesWriter(mapper: ObjectMapper) {

  def write[T](path: Path, records: Iterable[T]): Long = {
    val parent = path.toAbsolutePath.normalize().getParent

    if (parent != null) {
      Files.createDirectories(parent)
    }

    var recordCount = 0L

    val writer = Files.newBufferedWriter(
      path,
      StandardCharsets.UTF_8,
      StandardOpenOption.CREATE_NEW,
      StandardOpenOption.WRITE
    )

    try {
      records.iterator.foreach { record =>
        writer.write(mapper.writeValueAsString(record))
        writer.newLine()
        recordCount += 1
      }
    } finally {
      writer.close()
    }

    recordCount
  }
}
