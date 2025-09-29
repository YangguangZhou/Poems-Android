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
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.color.DynamicColors
import com.jerryz.poems.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity(), NavController.OnDestinationChangedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    // 定义在主导航图中的顶级目标ID列表（主页、收藏、搜索、关于）
    private val topLevelDestinations = setOf(
        R.id.homeFragment,
        R.id.favoriteFragment,
        R.id.searchFragment,
        R.id.aboutFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 应用 Material You 动态取色
        DynamicColors.applyToActivityIfAvailable(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置导航控制器
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // 添加导航变化监听器
        navController.addOnDestinationChangedListener(this)

        // 处理系统窗口插入
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigation) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bottom = insets.bottom)
            windowInsets
        }

        // 将底部导航栏与导航控制器关联
        binding.bottomNavigation.setupWithNavController(navController)

        // 设置底部导航栏的点击监听器
        setupBottomNavigationListener()

        //在Activity的onCreate()方法中调用
        WindowCompat.setDecorFitsSystemWindows(window, false)

        //设置导航栏为透明
        window.navigationBarColor = Color.TRANSPARENT;
        window.statusBarColor = Color.TRANSPARENT

        // 移除MainActivity层的返回处理，完全由BaseFragment处理
        // 这样可以确保预测性返回手势在所有Fragment中都能正常工作
        window.isNavigationBarContrastEnforced = false
    }

    private fun setupBottomNavigationListener() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val currentDestination = navController.currentDestination
            val currentId = currentDestination?.id
            if (currentId == item.itemId) {
                return@setOnItemSelectedListener true
            }

            // 如果当前不在顶层目标，则清空回退栈到顶层目标
            if (currentId !in topLevelDestinations) {
                navController.popBackStack(getStartDestination(), false)
            }

            // 在顶层导航间切换，防止创建新实例
            when (item.itemId) {
                R.id.homeFragment, R.id.favoriteFragment, R.id.searchFragment, R.id.aboutFragment -> {
                    if (currentId != item.itemId) {
                        navController.navigate(item.itemId)
                    }
                    true
                }
                else -> false
            }
        }
    }

    // 获取导航图的起始目标ID
    private fun getStartDestination(): Int {
        return navController.graph.startDestinationId
    }

    // 导航目标变化监听器实现
    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        // 根据当前目标决定是否显示底部导航栏
        val shouldShowBottomNav = destination.id in topLevelDestinations
        animateBottomNavigationVisibility(shouldShowBottomNav)

        // 如果当前是详情页面，更新系统UI配置
        if (destination.id == R.id.poemDetailFragment) {
            // 详情页面使用全屏模式，由Fragment自己处理
            WindowCompat.setDecorFitsSystemWindows(window, false)
        } else if (!shouldShowBottomNav) {
            // 其他非顶层页面，恢复正常显示
            WindowCompat.setDecorFitsSystemWindows(window, true)
        }
    }

    private fun animateBottomNavigationVisibility(show: Boolean) {
        val bottomNav = binding.bottomNavigation
        val currentVisibility = bottomNav.visibility

        // 如果已经是目标状态，则不执行动画
        if ((currentVisibility == View.VISIBLE && show) || (currentVisibility == View.GONE && !show)) {
            return
        }

        // 取消之前的动画
        bottomNav.clearAnimation()

        if (show) {
            com.jerryz.poems.util.AnimationUtils.slideInFromBottom(bottomNav, 300L)
        } else {
            com.jerryz.poems.util.AnimationUtils.slideOutToBottom(bottomNav, 250L)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}