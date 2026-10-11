package com.emberr.domain.util.media

import okio.Source
import okio.buffer
import okio.sink
import java.io.File

private const val STREAM_CHUNK_BYTES = 64L * 1024

internal fun copySourceIntoFile(openSource: () -> Source, file: File, onBytesCopied: (copiedBytes: Long) -> Unit) {
    openSource().use { source ->
        file.sink().buffer().use { sink ->
            var copiedBytes = 0L
            while (true) {
                val readBytes = source.read(sink.buffer, STREAM_CHUNK_BYTES)
                if (readBytes == -1L) break
                sink.emitCompleteSegments()
                copiedBytes += readBytes
                onBytesCopied(copiedBytes)
            }
        }
    }
}
