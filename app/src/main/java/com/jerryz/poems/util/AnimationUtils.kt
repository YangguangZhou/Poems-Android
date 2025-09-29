package com.jerryz.poems.util

import android.view.View
import android.view.animation.*

/**
 * 动画工具类，提供应用内一致的动画效果
 */
object AnimationUtils {
    
    // 标准动画持续时间
    const val DURATION_SHORT = 150L
    const val DURATION_MEDIUM = 250L
    const val DURATION_LONG = 375L
    
    // 标准插值器
    private val DECELERATE_INTERPOLATOR = DecelerateInterpolator(1.5f)
    private val ACCELERATE_INTERPOLATOR = AccelerateInterpolator(1.2f)
    private val OVERSHOOT_INTERPOLATOR = OvershootInterpolator(1.2f)
    private val ANTICIPATE_OVERSHOOT_INTERPOLATOR = AnticipateOvershootInterpolator(1.0f)
    
    /**
     * 为按钮添加按压动画效果
     */
    fun animateButtonPress(view: View, action: (() -> Unit)? = null) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100L)
            .setInterpolator(ACCELERATE_INTERPOLATOR)
            .withEndAction {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(100L)
                    .setInterpolator(DECELERATE_INTERPOLATOR)
                    .withEndAction { action?.invoke() }
                    .start()
            }
            .start()
    }
    
    /**
     * 为FloatingActionButton添加弹性按压动画
     */
    fun animateFabPress(view: View, action: (() -> Unit)? = null) {
        view.animate()
            .scaleX(0.90f)
            .scaleY(0.90f)
            .setDuration(120L)
            .setInterpolator(ACCELERATE_INTERPOLATOR)
            .withEndAction {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(DURATION_SHORT)
                    .setInterpolator(OVERSHOOT_INTERPOLATOR)
                    .withEndAction { action?.invoke() }
                    .start()
            }
            .start()
    }
    
    /**
     * 淡入动画
     */
    fun fadeIn(view: View, duration: Long = DURATION_MEDIUM) {
        view.alpha = 0f
        view.visibility = View.VISIBLE
        view.animate()
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(DECELERATE_INTERPOLATOR)
            .setListener(null)
            .start()
    }
    
    /**
     * 淡出动画
     */
    fun fadeOut(view: View, duration: Long = DURATION_MEDIUM, onEnd: (() -> Unit)? = null) {
        view.animate()
            .alpha(0f)
            .setDuration(duration)
            .setInterpolator(ACCELERATE_INTERPOLATOR)
            .withEndAction {
                view.visibility = View.GONE
                view.alpha = 1f // 重置透明度
                onEnd?.invoke()
            }
            .start()
    }
    
    /**
     * 从底部滑入动画
     */
    fun slideInFromBottom(view: View, duration: Long = DURATION_LONG) {
        view.visibility = View.VISIBLE
        view.translationY = view.height.toFloat()
        view.alpha = 0f
        view.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(DECELERATE_INTERPOLATOR)
            .setListener(null)
            .start()
    }
    
    /**
     * 向底部滑出动画
     */
    fun slideOutToBottom(view: View, duration: Long = DURATION_MEDIUM, onEnd: (() -> Unit)? = null) {
        view.animate()
            .translationY(view.height.toFloat())
            .alpha(0f)
            .setDuration(duration)
            .setInterpolator(ACCELERATE_INTERPOLATOR)
            .withEndAction {
                view.visibility = View.GONE
                view.translationY = 0f // 重置位置
                view.alpha = 1f // 重置透明度
                onEnd?.invoke()
            }
            .start()
    }
    
    /**
     * 缩放进入动画
     */
    fun scaleIn(view: View, duration: Long = DURATION_MEDIUM) {
        view.visibility = View.VISIBLE
        view.scaleX = 0.8f
        view.scaleY = 0.8f
        view.alpha = 0f
        view.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(ANTICIPATE_OVERSHOOT_INTERPOLATOR)
            .setListener(null)
            .start()
    }
    
    /**
     * 缩放退出动画
     */
    fun scaleOut(view: View, duration: Long = DURATION_SHORT, onEnd: (() -> Unit)? = null) {
        view.animate()
            .scaleX(0.8f)
            .scaleY(0.8f)
            .alpha(0f)
            .setDuration(duration)
            .setInterpolator(ACCELERATE_INTERPOLATOR)
            .withEndAction {
                view.visibility = View.GONE
                view.scaleX = 1f // 重置缩放
                view.scaleY = 1f
                view.alpha = 1f // 重置透明度
                onEnd?.invoke()
            }
            .start()
    }
    
    /**
     * 执行触觉反馈 - 基础版本
     */
    fun performHapticFeedback(view: View) {
        performHapticFeedback(view, HapticType.LIGHT)
    }
    
    /**
     * 震动反馈类型
     */
    enum class HapticType {
        LIGHT,          // 轻微震动 - 用于一般按钮点击
        MEDIUM,         // 中等震动 - 用于重要操作
        HEAVY,          // 重震动 - 用于确认或警告
        SUCCESS,        // 成功反馈 - 用于完成操作
        ERROR,          // 错误反馈 - 用于错误提示
        PARTIAL_ERROR,  // 部分错误 - 用于部分错误的情况
        DOUBLE_SUCCESS, // 双重成功震动 - 用于特别重要的成功
        TRIPLE_ERROR,   // 三重错误震动 - 用于严重错误
        STRONG_ERROR,   // 强力错误震动 - 用于需要强烈反馈的错误
        PLEASANT_SUCCESS // 愉悦成功震动 - 用于积极反馈
    }
    
    /**
     * 执行指定类型的触觉反馈
     */
    fun performHapticFeedback(view: View, type: HapticType) {
        when (type) {
            HapticType.SUCCESS -> {
                // 正确：一次长而柔和的震动
                performSingleHapticFeedback(view, HapticType.HEAVY)
            }
            HapticType.PARTIAL_ERROR -> {
                // 部分错误：两次中等强度震动
                performSingleHapticFeedback(view, HapticType.MEDIUM)
                view.postDelayed({
                    performSingleHapticFeedback(view, HapticType.MEDIUM)
                }, 100)
            }
            HapticType.TRIPLE_ERROR -> {
                // 完全错误：三次强烈震动
                performSingleHapticFeedback(view, HapticType.ERROR)
                view.postDelayed({
                    performSingleHapticFeedback(view, HapticType.ERROR)
                }, 80)
                view.postDelayed({
                    performSingleHapticFeedback(view, HapticType.ERROR)
                }, 160)
            }
            else -> {
                performSingleHapticFeedback(view, type)
            }
        }
    }
    
    /**
     * 执行单次触觉反馈
     */
    private fun performSingleHapticFeedback(view: View, type: HapticType) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            // Android S+ 支持更强的触觉反馈
            val feedbackConstant = when (type) {
                HapticType.LIGHT -> android.view.HapticFeedbackConstants.CLOCK_TICK
                HapticType.MEDIUM, HapticType.PARTIAL_ERROR -> android.view.HapticFeedbackConstants.CONTEXT_CLICK
                HapticType.HEAVY -> android.view.HapticFeedbackConstants.LONG_PRESS
                HapticType.SUCCESS, HapticType.DOUBLE_SUCCESS, HapticType.PLEASANT_SUCCESS -> android.view.HapticFeedbackConstants.CONFIRM
                HapticType.ERROR, HapticType.TRIPLE_ERROR, HapticType.STRONG_ERROR -> android.view.HapticFeedbackConstants.REJECT
            }
            // 对于强震动类型，执行两次以增强效果
            if (type in listOf(HapticType.HEAVY, HapticType.ERROR, HapticType.TRIPLE_ERROR, HapticType.STRONG_ERROR, HapticType.SUCCESS, HapticType.DOUBLE_SUCCESS)) {
                view.performHapticFeedback(feedbackConstant)
                view.postDelayed({
                    view.performHapticFeedback(feedbackConstant)
                }, 30)
            } else {
                view.performHapticFeedback(feedbackConstant)
            }
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // Android R+ 支持更丰富的触觉反馈
            val feedbackConstant = when (type) {
                HapticType.LIGHT -> android.view.HapticFeedbackConstants.CLOCK_TICK
                HapticType.MEDIUM, HapticType.PARTIAL_ERROR -> android.view.HapticFeedbackConstants.CONTEXT_CLICK
                HapticType.HEAVY -> android.view.HapticFeedbackConstants.LONG_PRESS
                HapticType.SUCCESS, HapticType.DOUBLE_SUCCESS, HapticType.PLEASANT_SUCCESS -> android.view.HapticFeedbackConstants.CONFIRM
                HapticType.ERROR, HapticType.TRIPLE_ERROR, HapticType.STRONG_ERROR -> android.view.HapticFeedbackConstants.REJECT
            }
            view.performHapticFeedback(feedbackConstant)
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            // Android M+ 支持基础触觉反馈
            val feedbackConstant = when (type) {
                HapticType.LIGHT -> android.view.HapticFeedbackConstants.CLOCK_TICK
                HapticType.MEDIUM, HapticType.SUCCESS, HapticType.PARTIAL_ERROR, HapticType.DOUBLE_SUCCESS, HapticType.PLEASANT_SUCCESS -> 
                    android.view.HapticFeedbackConstants.CONTEXT_CLICK
                HapticType.HEAVY, HapticType.ERROR, HapticType.TRIPLE_ERROR, HapticType.STRONG_ERROR -> 
                    android.view.HapticFeedbackConstants.LONG_PRESS
            }
            view.performHapticFeedback(feedbackConstant)
        } else {
            // 老版本Android的兼容处理
            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }
    
    /**
     * 组合动画：触觉反馈 + 按钮动画
     */
    fun animateButtonWithHaptic(view: View, action: (() -> Unit)? = null) {
        performHapticFeedback(view)
        animateButtonPress(view, action)
    }
    
    /**
     * 组合动画：触觉反馈 + FAB动画
     */
    fun animateFabWithHaptic(view: View, action: (() -> Unit)? = null) {
        performHapticFeedback(view)
        animateFabPress(view, action)
    }
    
    /**
     * 成功动画：弹性缩放进入，更欢快的效果
     */
    fun animateSuccess(view: View) {
        // 重置视图状态，确保动画可以重复播放
        view.clearAnimation()
        view.scaleX = 0.2f
        view.scaleY = 0.2f
        view.alpha = 0.5f
        
        view.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .alpha(1f)
            .setDuration(250L)
            .setInterpolator(OVERSHOOT_INTERPOLATOR)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120L)
                    .setInterpolator(DECELERATE_INTERPOLATOR)
                    .start()
            }
            .start()
    }
    
    /**
     * 部分错误动画：轻微摇摆
     */
    fun animatePartialError(view: View) {
        // 重置视图状态
        view.clearAnimation()
        view.rotation = 0f
        view.alpha = 1f
        
        view.animate()
            .rotationBy(3f)
            .setDuration(80)
            .setInterpolator(ACCELERATE_INTERPOLATOR)
            .withEndAction {
                view.animate()
                    .rotationBy(-6f)
                    .setDuration(120)
                    .setInterpolator(DECELERATE_INTERPOLATOR)
                    .withEndAction {
                        view.animate()
                            .rotationBy(3f)
                            .setDuration(80)
                            .setInterpolator(ACCELERATE_INTERPOLATOR)
                            .start()
                    }
                    .start()
            }
            .start()
    }
    
    /**
     * 错误动画：强烈摇摆，更明显的警告效果
     */
    fun animateError(view: View) {
        // 重置视图状态
        view.clearAnimation()
        view.translationX = 0f
        view.alpha = 1f
        
        // 更强烈的初始位移
        view.animate()
            .translationX(15f)
            .setDuration(40)
            .setInterpolator(ACCELERATE_INTERPOLATOR)
            .withEndAction {
                view.animate()
                    .translationX(-20f)
                    .setDuration(70)
                    .setInterpolator(DECELERATE_INTERPOLATOR)
                    .withEndAction {
                        view.animate()
                            .translationX(12f)
                            .setDuration(50)
                            .setInterpolator(ACCELERATE_INTERPOLATOR)
                            .withEndAction {
                                view.animate()
                                    .translationX(-8f)
                                    .setDuration(60)
                                    .setInterpolator(DECELERATE_INTERPOLATOR)
                                    .withEndAction {
                                        view.animate()
                                            .translationX(0f)
                                            .setDuration(80)
                                            .setInterpolator(DECELERATE_INTERPOLATOR)
                                            .start()
                                    }
                                    .start()
                            }
                            .start()
                    }
                    .start()
            }
            .start()
    }
}