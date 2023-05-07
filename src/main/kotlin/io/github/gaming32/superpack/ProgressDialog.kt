package io.github.gaming32.superpack

import com.jthemedetecor.OsThemeDetector
import io.github.gaming32.superpack.util.HasLogger
import io.github.gaming32.superpack.util.getLogger
import java.awt.BorderLayout
import java.awt.Frame
import javax.swing.*

class ProgressDialog(
    owner: Frame?, private val themeDetector: OsThemeDetector, title: String?, note: String?
) : JDialog(owner, title, true), HasLogger {
    private val themeListener: (Boolean) -> Unit = { isDark ->
        if (SuperpackSettings.INSTANCE.theme.isAffectedBySystem) {
            SwingUtilities.invokeLater {
                SuperpackSettings.INSTANCE.theme.systemThemeChanged(isDark)
                SwingUtilities.updateComponentTreeUI(this)
            }
        }
    }

    val note: JLabel
    val progressBar: JProgressBar
    private var openedOnce = false

    init {
        themeDetector.registerListener(themeListener)

        defaultCloseOperation = DISPOSE_ON_CLOSE

        this.note = JLabel(note)
        this.note.alignmentX = LEFT_ALIGNMENT

        progressBar = JProgressBar()
        progressBar.isStringPainted = true
        progressBar.alignmentX = LEFT_ALIGNMENT

        val cancelPanel = JPanel()
        cancelPanel.layout = BorderLayout()
        cancelPanel.border = BorderFactory.createEmptyBorder(0, 5, 5, 5)

        val cancelButton = JButton("Cancel")
        cancelButton.addActionListener { dispose() }
        cancelPanel.add(cancelButton, BorderLayout.EAST)
        cancelPanel.alignmentX = LEFT_ALIGNMENT

        val root = JPanel()
        root.layout = BoxLayout(root, BoxLayout.Y_AXIS)
        root.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

        root.add(Box.createVerticalGlue())
        root.add(this.note)
        root.add(Box.createVerticalStrut(5))
        root.add(progressBar)
        root.add(Box.createVerticalStrut(5))
        root.add(cancelPanel)
        root.add(Box.createVerticalGlue())

        contentPane = root
        pack()
    }

    override val logger get() = LOGGER

    override fun setVisible(value: Boolean) {
        if (value) {
            openedOnce = true
            themeDetector.registerListener(themeListener)
        } else {
            themeDetector.removeListener(themeListener)
        }
        super.setVisible(value)
    }

    fun setNote(note: String?) {
        this.note.text = note
        pack()
    }

    var progress by progressBar::value
    var maximum by progressBar::maximum

    var indeterminate
        get() = progressBar.isIndeterminate
        set(value) { progressBar.isIndeterminate = value }

    var string: String? by progressBar::string

    fun cancelled(): Boolean {
        return openedOnce && !isVisible
    }

    companion object {
        private val LOGGER = getLogger()
    }
}
