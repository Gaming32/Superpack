package io.github.gaming32.superpack.tabs

interface HasCachedScrollValue {
    var cachedScrollValue: Int
}

interface SelectedTabHandler {
    fun onSelected()
}
