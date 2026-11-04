package io.github.iammosab.tubeguard

import io.github.iammosab.tubeguard.shell.Shell
import io.github.iammosab.tubeguard.shell.ShellResult
import java.io.File

/** A TV whose `settings get/put/delete global|secure` really change [global] and [secure]; reads can be made to fail. */
class FakeTv(
    val global: MutableMap<String, String> = mutableMapOf(),
    val secure: MutableMap<String, String> = mutableMapOf(),
) : Shell {
    val commands = mutableListOf<String>()
    var failReads = false
    private val get = Regex("^settings get (global|secure) (\\S+)$")
    private val put = Regex("^settings put (global|secure) (\\S+) '(.*)'$")
    private val delete = Regex("^settings delete (global|secure) (\\S+)$")

    private fun table(name: String) = if (name == "global") global else secure

    override fun run(cmd: String): ShellResult {
        commands += cmd
        get.find(cmd)?.let {
            if (failReads) return ShellResult(-100, "ADB connection lost")
            return ShellResult(0, table(it.groupValues[1])[it.groupValues[2]] ?: "null")
        }
        put.find(cmd)?.let { table(it.groupValues[1])[it.groupValues[2]] = it.groupValues[3]; return ShellResult(0, "") }
        delete.find(cmd)?.let { table(it.groupValues[1]).remove(it.groupValues[2]); return ShellResult(0, "") }
        if (cmd.startsWith("getprop ro.product.manufacturer")) return ShellResult(0, "TCL")
        return ShellResult(0, "")
    }

    override fun install(apk: File) = ShellResult(1, "unused")
}
