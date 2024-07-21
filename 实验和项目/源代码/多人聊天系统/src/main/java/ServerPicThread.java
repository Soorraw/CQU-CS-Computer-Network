import net.sf.json.JSONObject;

import javax.swing.text.BadLocationException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ServerPicThread extends Thread {


    ServerSocket server = null;
    Socket socket = null;
    static List<User> list = new ArrayList<User>();  // 存储客户端

    public void remove(int i) {
        list.remove(i);
    }

    public void run() {
        try {
            server = new ServerSocket(Utils.ServerPicThreadPort);
            System.out.println("ServerPicThread启动成功");

            while (true) {
                System.out.println("图片传输socket列表启动");
                socket = server.accept();

                // 获取输入流
                DataInputStream in = new DataInputStream(socket.getInputStream());
                // 读取输入流
                String json = in.readUTF();
                // 转换为JSON对象
                JSONObject data = JSONObject.fromObject(json);

                // 创建新用户
                User user = new User();
                // 设置新用户socket属性
                user.setSocket(socket);
                // 获取输入流中的用户名并赋值给创建的新用户对象
//                System.out.println(data.toString());
                user.setUsername(data.getString("username"));
                // 添加新用户对象到用户列表
                list.add(user);
//                System.out.println("添加新用户后的图片用户列表" + list);

                System.out.println("与服务器端图片传输socket连接成功");
                // 开启图片传输线程
                PicReadAndWrite picReadAndWrite = new PicReadAndWrite(socket);
                picReadAndWrite.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class PicReadAndWrite extends Thread {
    private Socket nowSocket;
    private DataInputStream input = null;
    private DataOutputStream output = null;

    public PicReadAndWrite(Socket socket) {
        this.nowSocket = socket;
    }

    public void run() {
        try {
            input = new DataInputStream(nowSocket.getInputStream());  // 输入流
            while (true) {

                String time = input.readUTF();

                // 解读文件名
                String textName = input.readUTF();

                // 解读文件长度
                long textLength = input.readLong();

                // 解读目的对象类型
                String currentChannelType = input.readUTF();
                System.out.println("解读目标对象类型：" + currentChannelType);

                // 解读文件目标对象
                String currentObject = input.readUTF();
                System.out.println("解读目标对象：" + currentObject);

                String sourceObejct = input.readUTF();

                try { // 插入文本
                    Server.show_area.insertString(Server.show_area.getLength(),
                            "[" + currentObject + "] " + sourceObejct + " " + time + "\n" + "[图片] " + textName + "\n", null);
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }

                if (currentChannelType.equals("private")) {
                    if (currentObject.equals("world")) {
                        System.out.println("进行图片群发");
                        for (User u : ServerPicThread.list) {
                            output = new DataOutputStream(u.getSocket().getOutputStream());  // 输出流
                              // 发送给其它客户端
                            send(time,textName,textLength,currentChannelType,currentObject,sourceObejct);
                        }
                        // 发送文件内容
                        int length = -1;
                        long curLength = 0;
                        byte[] buff = new byte[1024];
                        while ((length = input.read(buff)) > 0) {
                            curLength += length;
                            for (User u : ServerPicThread.list) {
                                output = new DataOutputStream(u.getSocket().getOutputStream());  // 输出流
                                  // 发送给其它客户端
                                    output.write(buff, 0, length);
                                    output.flush();

                            }
                            if (curLength == textLength) {  // 强制退出
                                break;
                            }
                        }

                    } else {
                        System.out.println("进行图片私发");
//                        System.out.println(ServerPicThread.list.size());

                        output = new DataOutputStream(nowSocket.getOutputStream());
                        send(time,textName,textLength,currentChannelType,currentObject,sourceObejct);

                        for (User u : ServerPicThread.list) {
                            if (u.getUsername().equals(currentObject)) {
                                output = new DataOutputStream(u.getSocket().getOutputStream());  // 输出流
                                send(time,textName,textLength,currentChannelType,sourceObejct, sourceObejct);
                            }
                        }
                        // 发送文件内容
                        int length = -1;
                        long curLength = 0;
                        byte[] buff = new byte[1024];
                        while ((length = input.read(buff)) > 0) {
                            curLength += length;
                            for (User u : ServerPicThread.list) {
                                if (u.getUsername().equals(currentObject) || u.getSocket()==nowSocket) {
                                    output = new DataOutputStream(u.getSocket().getOutputStream());
                                    output.write(buff, 0, length);
                                    output.flush();
                                }
                            }
                            if (curLength == textLength) {  // 强制退出
                                break;
                            }
                        }
                    }
                } else {
                    System.out.println("进行图片组发：" + currentObject);
                    String[] users = currentObject.split("#");

                    System.out.println(ServerPicThread.list.size());

                    for (User u : ServerPicThread.list) {

                        for (String s : users) {
                            if (u.getUsername().equals(s)) {
                                output = new DataOutputStream(u.getSocket().getOutputStream());  // 输出流
                                send(time,textName,textLength,currentChannelType,currentObject,sourceObejct);
                            }
                        }
                    }
                    // 发送文件内容
                    int length = -1;
                    long curLength = 0;
                    byte[] buff = new byte[1024];
                    while ((length = input.read(buff)) > 0) {
                        curLength += length;

                        for (String s : users) {
                            for (User u : ServerPicThread.list) {
                                if (u.getUsername().equals(s)) {
                                    output = new DataOutputStream(u.getSocket().getOutputStream());
                                    output.write(buff, 0, length);
                                    output.flush();
                                }
                            }
                        }
                        if (curLength == textLength) {  // 强制退出
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 线程关闭
        }
    }
    private void send(String time, String textName, long textLength, String currentChannelType, String currentObject, String sourceObject){
        try {
            output.writeUTF(time);
            output.flush();
            // 封装文件名
            output.writeUTF(textName);
            output.flush();
            // 封装文件长度
            output.writeLong(textLength);
            output.flush();
            output.writeUTF(currentChannelType);
            output.flush();
            output.writeUTF(currentObject);
            output.flush();
            output.writeUTF(sourceObject);
            output.flush();
        }catch (Exception e){

        }
    }
}

