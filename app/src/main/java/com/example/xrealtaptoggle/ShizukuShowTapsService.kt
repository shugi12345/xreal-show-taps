package com.example.xrealtaptoggle

class ShizukuShowTapsService : IShowTapsService.Stub() {
    override fun setShowTaps(enabled: Boolean): Boolean {
        val value = if (enabled) "1" else "0"
        return runSettingsCommand("put", "system", "show_touches", value).exitCode == 0
    }

    override fun getShowTaps(): Int {
        val result = runSettingsCommand("get", "system", "show_touches")
        if (result.exitCode != 0) {
            return -1
        }

        return when (result.stdout.trim()) {
            "1" -> 1
            "0" -> 0
            else -> -1
        }
    }

    override fun destroy() {
        System.exit(0)
    }

    private fun runSettingsCommand(vararg args: String): CommandResult {
        val process = ProcessBuilder(listOf("/system/bin/settings", *args)).start()
        val stdout = process.inputStream.bufferedReader().use { it.readText() }
        val stderr = process.errorStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()
        return CommandResult(exitCode, stdout, stderr)
    }

    private data class CommandResult(
        val exitCode: Int,
        val stdout: String,
        val stderr: String,
    )
}
