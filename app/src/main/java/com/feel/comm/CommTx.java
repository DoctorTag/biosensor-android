

package com.feel.comm;


public class CommTx {

   public static final byte SET_WIFI_CONFIG	= 0x30;
    public static final byte IPADDR_IND	= 0x31;
    private  byte[] sendbuf ;
    public CommTx() {
        sendbuf = null;
    }
    public synchronized  byte[] commTxMakeFrame(byte ctrlCode, byte[] data1) {
        sendbuf = null;
        if (data1 != null)
            sendbuf = new byte[5 + data1.length];
        else
            sendbuf = new byte[5];
        short size = 0;
        sendbuf[size++] = CommRx.START_FRAME_HEADER;
        sendbuf[size++] = ctrlCode;
        if (data1 == null) {
            sendbuf[size++] = 0;
            sendbuf[size++] = 0;  //CRC
        } else {
            sendbuf[size++] = (byte) data1.length;
            byte tcrc = 0;
            for (short i = 0; i < data1.length; i++) {
                sendbuf[size++] = data1[i];
                tcrc ^= data1[i];
            }
            sendbuf[size++] = tcrc;
        }
        sendbuf[size] = CommRx.START_FRAME_END;
        return sendbuf;

    }



}