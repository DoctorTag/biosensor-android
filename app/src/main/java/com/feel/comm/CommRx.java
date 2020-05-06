

package com.feel.comm;


import android.annotation.SuppressLint;
import android.os.Handler;
import android.util.Log;
//import android.widget.Toast;
//import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for incoming
 * connections, a thread for connecting with a device, and a thread for
 * performing data transmissions when connected.
 */

public class CommRx {
    public static final byte START_FRAME_HEADER = 0x55;
    public static final byte START_FRAME_END = 0x0A;
    private static final byte RX_HEADER = 0x00;
    private static final byte RX_CMD = 0x01;
    private static final byte RX_LENGTH = 0x02;
    private static final byte RX_DATA = 0x03;
    private static final byte RX_CRC = 0x04;
    private static final byte RX_END = 0x05;

    private byte ctype;
    private byte dframe[];
    private byte rx_state;
    private byte lcrc;
    //private byte rcrc;
    private short rlength;
    private short offset;

    private static final short PACK_SAMPLES = 100;

    private static final short MAX_RDATA_LENGTH = (3 * PACK_SAMPLES + 1);
    private static final short MAX_RX_LENGTH = (MAX_RDATA_LENGTH + 5); /*1 Start+1CMD+1DLEN+MAX_TDATA+2CRC*/


    public CommRx() {
        RecvInit();
        dframe = new byte[MAX_RX_LENGTH];
    }

    /**
     * Return the current connection state.
     *
     * @throws UnsupportedEncodingException
     */

    private synchronized void ProcessRecvFrame(Handler handler, byte[] frame,
                                               short fsize) throws UnsupportedEncodingException {
        switch (frame[1]) {

            case 0:
                break;


        }

    }

    private static int unsignedByteToInt(byte b) {
        return (int) b & 0xFF;
    }


    void RecvInit() {
        rx_state = RX_HEADER;
        offset = 0;

    }

    public synchronized byte[] RecvStreamFrame(byte src) {
        byte ret[] = null;

        dframe[offset] = src;
        offset++;


        switch (rx_state) {
            case RX_HEADER:
                if (dframe[offset - 1] == START_FRAME_HEADER) {
                    rx_state = RX_CMD;
                    rlength = 0;

                } else if (dframe[offset - 1] == (START_FRAME_HEADER + 1)) {
                    rx_state = RX_CMD;
                    rlength = 256;

                } else {
                    RecvInit();
                }

                break;
            case RX_CMD:
                rx_state = RX_LENGTH;
                break;
            case RX_LENGTH:
                rlength += unsignedByteToInt(dframe[offset - 1]);
                if (rlength <= MAX_RDATA_LENGTH) {
                    rx_state = RX_DATA;
                    lcrc = 0;
                } else {
                    RecvInit();
                }
                break;
            case RX_DATA:
                if (offset >= (rlength + 3)) {
                    rx_state = RX_CRC;

                }
                lcrc ^= dframe[offset - 1];
                break;
            case RX_CRC:
                if (offset == (rlength + 4)) {
                   // rcrc = dframe[offset - 1];
                    //ProcessRecvFrame(handler, dframe, offset);
                    if (dframe[offset - 1] == lcrc)
                        dframe[offset - 1] = 1;
                    else
                        dframe[offset - 1] = 0;
                    ret = dframe;
                    RecvInit();
                }


                if (offset > (rlength + 4)) {
                    RecvInit();
                }
                break;

        }

        return ret;
    }
}