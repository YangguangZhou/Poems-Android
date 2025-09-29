package com.jerryz.poems.ui.base

import android.os.Build
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

/**
 * 基础Fragment，提供统一的预测性返回手势支持
 */
abstract class BaseFragment : Fragment() {

    private var onBackPressedCallback: OnBackPressedCallback? = null

    override fun onStart() {
        super.onStart()
        setupPredictiveBackGesture()
    }

    override fun onStop() {
        super.onStop()
        onBackPressedCallback?.remove()
        onBackPressedCallback = null
    }

    /**
     * 设置预测性返回手势
     */
    private fun setupPredictiveBackGesture() {
        // 对于所有Android版本都设置OnBackPressedCallback，确保预测性返回正常工作
        // Android 13+会自动启用预测性返回动画，早期版本使用标准返回逻辑
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 子类可以重写 handleBackPress() 来自定义返回行为
                if (!handleBackPress()) {
                    // 使用Navigation组件的标准返回逻辑
                    if (!findNavController().navigateUp()) {
                        // 如果无法返回上级，则退出Activity
                        requireActivity().finish()
                    }
                }
            }
        }
        
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            onBackPressedCallback!!
        )
    }

    /**
     * 子类可以重写此方法来处理自定义的返回逻辑
     * @return true 表示已处理返回事件，false 表示使用默认返回逻辑
     */
    protected open fun handleBackPress(): Boolean {
        return false
    }
}