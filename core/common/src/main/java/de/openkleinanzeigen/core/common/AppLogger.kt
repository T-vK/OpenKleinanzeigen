package de.openkleinanzeigen.core.common

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

object AppLogger {
    private const val MAX_LINES = 8000
    private const val LOGCAT_CHUNK = 3500
    private val listeners = CopyOnWriteArrayList<(String) -> Unit>()
    private val buffer = ArrayDeque<String>(MAX_LINES)
    private var logFile: File? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val enabled = AtomicBoolean(false)

    fun init(file: File) {
        logFile = file
    }

    fun setEnabled(value: Boolean) {
        enabled.set(value)
        if (value) {
            i("AppLogger", "Debug logging enabled")
        }
    }

    fun isEnabled(): Boolean = enabled.get()

    fun d(tag: String, message: String) = log("D", tag, message)
    fun i(tag: String, message: String) = log("I", tag, message)
    fun w(tag: String, message: String) = log("W", tag, message)
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        log("E", tag, message)
        throwable?.let { log("E", tag, it.stackTraceToString()) }
    }

    private fun log(level: String, tag: String, message: String) {
        if (!enabled.get()) return
        val timestamp = dateFormat.format(Date())
        message.lineSequence().forEach { line ->
            val entry = "$timestamp $level/$tag: $line"
            synchronized(buffer) {
                if (buffer.size >= MAX_LINES) buffer.removeFirst()
                buffer.addLast(entry)
            }
            logFile?.appendText("$entry\n")
            listeners.forEach { it(entry) }
            logToLogcat(level, tag, line)
        }
    }

    private fun logToLogcat(level: String, tag: String, message: String) {
        val priority = when (level) {
            "E" -> android.util.Log.ERROR
            "W" -> android.util.Log.WARN
            "I" -> android.util.Log.INFO
            else -> android.util.Log.DEBUG
        }
        if (message.length <= LOGCAT_CHUNK) {
            android.util.Log.println(priority, tag, message)
            return
        }
        var offset = 0
        var part = 1
        while (offset < message.length) {
            val end = minOf(offset + LOGCAT_CHUNK, message.length)
            android.util.Log.println(priority, tag, "[$part] ${message.substring(offset, end)}")
            offset = end
            part++
        }
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
