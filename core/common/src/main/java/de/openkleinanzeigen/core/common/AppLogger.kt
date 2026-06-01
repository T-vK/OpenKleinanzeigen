package de.openkleinanzeigen.core.common

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

object AppLogger {
    private const val MAX_LINES = 2000
    private val listeners = CopyOnWriteArrayList<(String) -> Unit>()
    private val buffer = ArrayDeque<String>(MAX_LINES)
  private var logFile: File? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun init(file: File) {
        logFile = file
    }

    fun d(tag: String, message: String) = log("D", tag, message)
    fun i(tag: String, message: String) = log("I", tag, message)
    fun w(tag: String, message: String) = log("W", tag, message)
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val extra = throwable?.let { "\n${it.stackTraceToString()}" }.orEmpty()
        log("E", tag, message + extra)
    }

    private fun log(level: String, tag: String, message: String) {
        val line = "${dateFormat.format(Date())} $level/$tag: $message"
        synchronized(buffer) {
            if (buffer.size >= MAX_LINES) buffer.removeFirst()
            buffer.addLast(line)
        }
        logFile?.appendText("$line\n")
        listeners.forEach { it(line) }
        android.util.Log.println(
            when (level) {
                "E" -> android.util.Log.ERROR
                "W" -> android.util.Log.WARN
                else -> android.util.Log.DEBUG
            },
            tag,
            message,
        )
    }

    fun addListener(listener: (String) -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: (String) -> Unit) {
        listeners.remove(listener)
    }

    fun getLogs(): String = synchronized(buffer) { buffer.joinToString("\n") }

    fun clear() {
        synchronized(buffer) { buffer.clear() }
        logFile?.writeText("")
    }
}
