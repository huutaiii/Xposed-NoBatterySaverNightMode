// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2021 Truong Quoc Tai

package ht.android.xposed.nobatterysavernightmode

import android.content.Context
import android.content.res.Configuration
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_LoadPackage

class Module : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        when (lpparam.packageName) {
            "android" -> {
                XposedHelpers.findAndHookMethod(
                        "com.android.server.UiModeManagerService",
                        lpparam.classLoader,
                        "updateConfigurationLocked",
                        object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                XposedHelpers.setBooleanField(param.thisObject, "mPowerSave", false)
                            }
                        })
            }
            "com.android.settings" -> {
                XposedHelpers.findAndHookMethod(
                        "com.android.settings.display.darkmode.DarkModeObserver",
                        lpparam.classLoader,
                        "subscribe",
                        Runnable::class.java,
                        object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                param.result = null
                            }
                        })
                XposedHelpers.findAndHookMethod(
                        "com.android.settings.display.DarkUIPreferenceController",
                        lpparam.classLoader, "isPowerSaveMode",
                        object : XC_MethodReplacement() {
                            override fun replaceHookedMethod(param: MethodHookParam): Any {
                                return false
                            }
                        })
                val classActivation = XposedHelpers.findClass("com.android.settings.display.darkmode.DarkModeActivationPreferenceController", lpparam.classLoader)
                XposedBridge.hookAllMethods(
                        classActivation,
                        "updateState",
                        object : XC_MethodHook() {
                            override fun afterHookedMethod(param: MethodHookParam) {
                                val mPowerManager = XposedHelpers.getObjectField(param.thisObject, "mPowerManager")
                                val batterySaver = XposedHelpers.callMethod(mPowerManager, "isPowerSaveMode") as Boolean
                                if (batterySaver) {
                                    val context = XposedHelpers.getObjectField(param.thisObject, "mContext") as Context
                                    val active = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_YES != 0
                                    XposedHelpers.callMethod(param.thisObject, "updateNightMode", active)
                                }
                            }
                        }
                )
                val classSchedule = XposedHelpers.findClass("com.android.settings.display.darkmode.DarkModeScheduleSelectorController", lpparam.classLoader)
                XposedBridge.hookAllMethods(
                        classSchedule,
                        "updateState",
                        object : XC_MethodHook() {
                            override fun afterHookedMethod(param: MethodHookParam) {
                                val mPreference = XposedHelpers.getObjectField(param.thisObject, "mPreference")
                                XposedHelpers.callMethod(mPreference, "setEnabled", true)
                            }
                        }
                )
            }
            "com.android.systemui" -> {
                XposedHelpers.findAndHookMethod(
                        "com.android.systemui.qs.tiles.UiModeNightTile",
                        lpparam.classLoader,
                        "handleUpdateState",
                        XposedHelpers.findClass("com.android.systemui.plugins.qs.QSTile.BooleanState", lpparam.classLoader),
                        Object::class.java,
                        object : XC_MethodHook() {
                            override fun afterHookedMethod(param: MethodHookParam) {
                                val state = param.args[0]
                                val classTile = XposedHelpers.findClass("android.service.quicksettings.Tile", lpparam.classLoader)
                                val value = XposedHelpers.getBooleanField(state, "value")
                                val active = XposedHelpers.getStaticIntField(classTile, "STATE_ACTIVE")
                                val inactive = XposedHelpers.getStaticIntField(classTile, "STATE_INACTIVE")
                                XposedHelpers.setIntField(state, "state", if (value) active else inactive)
                            }
                        }
                )
            }
        }
    }
}