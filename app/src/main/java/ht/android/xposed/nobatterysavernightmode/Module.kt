// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2021 Truong Quoc Tai

package ht.android.xposed.nobatterysavernightmode

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/*
TODO:
    - Re-enable setting (and quicksettings tile)
*/
class Module : IXposedHookLoadPackage
{
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam)
    {
        when (lpparam.packageName)
        {
            "android" ->
            {
                val className ="com.android.server.UiModeManagerService"
                val methodName = "updateConfigurationLocked"
                XposedBridge.log("Hooking method: $className#$methodName")
                XposedHelpers.findAndHookMethod(className, lpparam.classLoader, methodName, object: XC_MethodHook()
                {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        super.beforeHookedMethod(param)
                        XposedHelpers.setBooleanField(param.thisObject, "mPowerSave", false)
                        XposedBridge.log("Hooked method \"${param.method.toString()}\" was called")
                    }
                })
            }
        }
    }
}