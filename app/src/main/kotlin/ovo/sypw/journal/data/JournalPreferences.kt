package ovo.sypw.journal.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import ovo.sypw.journal.utils.SnackBarUtils

class JournalPreferences(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    private val keyFirstLaunch = "is_first_launch"

    /**
     * 检查是否是第一次启动
     */
    fun isFirstLaunch(): Boolean {
        val isFirstLaunch = sharedPreferences.getBoolean(keyFirstLaunch, true)
        if (isFirstLaunch) {
            SnackBarUtils.showSnackBar("First Launch")
            sharedPreferences.edit { putBoolean(keyFirstLaunch, false) }
        } else {
            SnackBarUtils.showSnackBar("Not First Launch")
        }
        return isFirstLaunch
    }
}