package com.github.akshayashokcode.devfocus.util

import java.io.BufferedInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.LineEvent

object SoundPlayer {

    @Synchronized
    fun play(soundFileName: String) {

        try {

            val resourceStream = SoundPlayer::class.java
                .getResourceAsStream("/sounds/$soundFileName")
                ?: return

            val bufferedStream = BufferedInputStream(resourceStream)

            val audioInputStream =
                AudioSystem.getAudioInputStream(bufferedStream)

            val clip: Clip = AudioSystem.getClip()

            clip.open(audioInputStream)

            clip.addLineListener { event ->

                if (event.type == LineEvent.Type.STOP) {

                    clip.stop()
                    clip.flush()
                    clip.close()

                    audioInputStream.close()
                    bufferedStream.close()
                    resourceStream.close()
                }
            }

            clip.start()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}