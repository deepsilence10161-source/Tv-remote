package com.tvremote.controller.protocol

/** Android KeyEvent keycodes used by the remote (confirmed against the reference app). */
object KeyCodes {
    const val HOME = 3
    const val BACK = 4
    const val DPAD_UP = 19
    const val DPAD_DOWN = 20
    const val DPAD_LEFT = 21
    const val DPAD_RIGHT = 22
    const val DPAD_CENTER = 23
    const val VOLUME_UP = 24
    const val VOLUME_DOWN = 25
    const val POWER = 26
    const val MENU = 82
    const val TV_INPUT = 178

    // extra keys we add for our own agent
    const val MUTE = 164
    const val PLAY_PAUSE = 85
    const val MEDIA_NEXT = 87
    const val MEDIA_PREVIOUS = 88
    const val SEARCH = 84
    const val SETTINGS = 176
}
