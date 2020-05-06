

package com.feel.comm;


import android.widget.Toast;

import com.xuhao.didi.socket.client.sdk.OkSocket;
import com.xuhao.didi.socket.client.sdk.client.ConnectionInfo;
import com.xuhao.didi.socket.client.sdk.client.OkSocketOptions;
import com.xuhao.didi.socket.client.sdk.client.connection.IConnectionManager;

public class CommTcp {
private String ip_addr;
    public IConnectionManager manager;
    public CommTcp(String ip_a) {
        ip_addr = ip_a;
        //连接参数设置(IP,端口号),这也是一个连接的唯一标识,不同连接,该参数中的两个值至少有其一不一样
        ConnectionInfo info = new ConnectionInfo(ip_addr, 3338);
        //调用OkSocket,开启这次连接的通道,拿到通道Manager
        manager = OkSocket.open(info);


        //获得当前连接通道的参配对象
        OkSocketOptions options= manager.getOption();
        //基于当前参配对象构建一个参配建造者类
        OkSocketOptions.Builder builder = new OkSocketOptions.Builder(options);
//修改参配设置(其他参配请参阅类文档)
        //builder.setSinglePackageBytes(size);
//建造一个新的参配对象并且付给通道
        manager.option(builder.build());

    }
 //注册Socket行为监听器,SocketActionAdapter是回调的Simple类,其他回调方法请参阅类文档
    /*
manager.registerReceiver(new SocketActionAdapter(){
        @Override
        public void onSocketConnectionSuccess(ConnectionInfo info, String action) {
            Toast.makeText(context, "连接成功", LENGTH_SHORT).show();
        }
    });
//调用通道进行连接
manager.connect();
*/

    }



