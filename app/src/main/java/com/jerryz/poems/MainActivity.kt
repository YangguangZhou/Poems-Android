package com.jerryz.poems

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.window.OnBackInvokedDispatcher
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.color.DynamicColors
import com.jerryz.poems.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 应用 Material You 动态取色
        DynamicColors.applyToActivityIfAvailable(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置导航控制器
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // 处理系统窗口插入
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigation) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bottom = insets.bottom)
            windowInsets
        }

        // 将底部导航栏与导航控制器关联
        binding.bottomNavigation.setupWithNavController(navController)
        //在Activity的onCreate()方法中调用
        WindowCompat.setDecorFitsSystemWindows(window, false);

        //设置导航栏为透明
        window.setNavigationBarColor(Color.TRANSPARENT);
        window.setStatusBarColor(Color.TRANSPARENT);

        // 添加预见式返回手势支持
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (Build.VERSION.SDK_INT >= 34) { // Android 14+
                onBackInvokedDispatcher.registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT
                ) {
                    // 通过导航控制器处理返回逻辑
                    if (!navController.popBackStack()) {
                        finish()
                    }
                }
            } else {
                // Android 13 的处理方式
                onBackPressedDispatcher.addCallback(this) {
                    if (!navController.popBackStack()) {
                        finish()
                    }
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //当背景透明时去掉灰色蒙层
            window.isNavigationBarContrastEnforced = false
        }
        //导航栏颜色透明
        window.navigationBarColor = Color.TRANSPARENT
    }
}