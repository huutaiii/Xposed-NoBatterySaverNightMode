// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2021 Truong Quoc Tai

package ht.android.xposed.nobatterysavernightmode

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
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
                            super.beforeHookedMethod(param)
                            XposedHelpers.setBooleanField(param.thisObject, "mPowerSave", false)
                            XposedBridge.log("Hooked method \"${param.method.toString()}\" was called")
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
                            super.beforeHookedMethod(param)
                            param.result = null
                        }
                    })
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
                            super.afterHookedMethod(param)
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