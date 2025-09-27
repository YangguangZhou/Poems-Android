package com.jerryz.poems.ui.about

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.jerryz.poems.BuildConfig
import com.jerryz.poems.R
import com.jerryz.poems.databinding.FragmentAboutBinding
import com.google.android.material.snackbar.Snackbar
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class AboutFragment : Fragment() {

    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.aboutAppBarLayout) { appBar, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            appBar.updatePadding(top = statusBars.top)
            insets
        }
        ViewCompat.requestApplyInsets(binding.aboutAppBarLayout)

        loadAppVersion()
        loadAuthorImage()
        setupClickListeners()
    }

    private fun loadAppVersion() {
        val context = requireContext()
        val packageManager = context.packageManager

        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(context.packageName, 0)
            }

            binding.textVersionName.text = packageInfo.versionName ?: getString(R.string.about_build_unknown)

            val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
            binding.textVersionCode.text = versionCode.toString()

            val buildType = BuildConfig.BUILD_TYPE.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
            binding.textBuildType.text = buildType

            val lastUpdate = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(packageInfo.lastUpdateTime))
            binding.textLastUpdate.text = lastUpdate
        } catch (e: PackageManager.NameNotFoundException) {
            binding.textVersionName.text = getString(R.string.about_build_unknown)
            binding.textVersionCode.text = getString(R.string.about_build_unknown)
            binding.textBuildType.text = getString(R.string.about_build_unknown)
            binding.textLastUpdate.text = getString(R.string.about_build_unknown)
        }
    }

    private fun loadAuthorImage() {
        Glide.with(this)
            .load("https://img.examcoo.com/ask/7386438/202111/163626915705190.jpg")
            .placeholder(R.drawable.author_avatar)
            .error(R.drawable.error_avatar)
            .circleCrop()
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
            .skipMemoryCache(false)
            .into(binding.imageAuthor)
    }

    private fun setupClickListeners() {
        // Developer links
        binding.buttonWebsite.setOnClickListener {
            openUrl("https://jerryz.com.cn")
        }

        binding.buttonBlog.setOnClickListener {
            openUrl("https://blog.jerryz.com.cn")
        }

        binding.buttonGitHub.setOnClickListener {
            openUrl("https://github.com/YangguangZhou")
        }

        // App links
        binding.buttonWebVersion.setOnClickListener {
            openUrl("https://poems.jerryz.com.cn")
        }

        // Feedback options - Modified to copy to clipboard
        binding.buttonEmail.setOnClickListener {
            copyToClipboard("Email", "i@jerryz.com.cn")
            showSuccessSnackbar(R.string.email_copied)
        }

        binding.buttonQQ.setOnClickListener {
            copyToClipboard("QQ", "2098412009")
            showSuccessSnackbar(R.string.qq_copied)
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            showErrorSnackbar(R.string.error_opening_url)
        }
    }

    /**
     * Copy text to clipboard
     */
    private fun copyToClipboard(label: String, text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }

    private fun showSuccessSnackbar(messageResId: Int) {
        Snackbar.make(binding.root, messageResId, Snackbar.LENGTH_SHORT).show()
    }

    private fun showErrorSnackbar(messageResId: Int) {
        Snackbar.make(binding.root, messageResId, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
