package com.kieronquinn.app.utag.ui.screens.settings.faq

import android.os.Bundle
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.updatePadding
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.databinding.FragmentFaqBinding
import com.kieronquinn.app.utag.ui.base.BackAvailable
import com.kieronquinn.app.utag.ui.base.BoundFragment
import com.kieronquinn.app.utag.utils.extensions.SYSTEM_INSETS
import com.kieronquinn.app.utag.utils.extensions.getAttrColor
import com.kieronquinn.app.utag.utils.extensions.onApplyInsets
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.tables.TablePlugin
import org.commonmark.node.Heading

class SettingsFaqFragment: BoundFragment<FragmentFaqBinding>(FragmentFaqBinding::inflate), BackAvailable {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val typeface = ResourcesCompat.getFont(requireContext(), R.font.oneui_sans_medium)
        val markwon = Markwon.builder(requireContext()).usePlugin(object : AbstractMarkwonPlugin() {
            override fun configureTheme(builder: MarkwonTheme.Builder) {
                typeface?.let {
                    builder.headingTypeface(it)
                    builder.headingBreakHeight(0)
                }
            }

            override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
                val origin = builder.requireFactory(Heading::class.java)
                builder.setFactory(Heading::class.java) { configuration, props ->
                    arrayOf(
                        origin.getSpans(configuration, props),
                        ForegroundColorSpan(
                            requireContext().getAttrColor(android.R.attr.textColorPrimary)
                        )
                    )
                }
            }
        }).usePlugin(TablePlugin.create(requireContext())).usePlugin(CorePlugin.create()).build()
        val markdown = requireContext().resources.openRawResource(R.raw.faq).bufferedReader()
            .use { it.readText() }
        markwon.setMarkdown(binding.markdown, markdown)
        binding.markdown.setLinkTextColor(
            ContextCompat.getColor(requireContext(), R.color.oui_accent_color)
        )
        binding.root.onApplyInsets { root, insets ->
            val padding = resources.getDimensionPixelSize(R.dimen.margin_16)
            val inset = insets.getInsets(SYSTEM_INSETS)
            root.updatePadding(bottom = inset.bottom + padding)
        }
    }

}