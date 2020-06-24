/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import javax.sound.sampled.*;
import javax.sound.sampled.AudioSystem;
import java.io.*;

/**
 *
 * @author 15002
 */
public class Sound {


    public static float SAMPLERATE = 8000f;

    public static void changeFreq(int hz, int ms) throws LineUnavailableException {
        byte[] buf = new byte[(int) SAMPLERATE * ms / 1000];
        if (hz > 1000) {
            hz = 1000;
        }
        for (int i = 0; i < buf.length; i++) {
            double angle = i / (SAMPLERATE / hz) * 2.0 * Math.PI;
            buf[i] = (byte) (Math.sin(angle) * 138.0 * 1);
        }

        for (int i = 0; i < SAMPLERATE / 100.0 && i < buf.length / 2; i++) {
            buf[i] = (byte) (buf[i] * i / (SAMPLERATE / 100.0));
            buf[buf.length - 1 - i]
                    = (byte) (buf[buf.length - 1 - i] * i / (SAMPLERATE / 100.0));
        }

        AudioFormat af = new AudioFormat(SAMPLERATE, 8, 1, true, false);
        SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
        sdl.open(af);
        sdl.start();
        sdl.write(buf, 0, buf.length);
//        sdl.drain();
//        sdl.close();
    }

    public void play(int i) {
        try {
            changeFreq(200 + i * 5, 100);
        } catch (LineUnavailableException e) {
        }
    }
}
