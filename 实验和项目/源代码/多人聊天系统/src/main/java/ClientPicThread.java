import net.sf.json.JSONObject;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;

public class ClientPicThread extends Thread {

    private Socket socket = null;
    static String userName = null;
    static DataInputStream fileIn = null;
    static DataOutputStream fileOut = null;
    static DataInputStream fileReader = null;
    static DataOutputStream fileWriter = null;

    public ClientPicThread(String userName) {
        ClientPicThread.userName = userName;
    }

    // 客户端发送图片
    static void outPicToServer(String path, String currentChannelType, String currentObject) {
        try {
            System.out.println("客户端上传图片");
            File file = new File(path);
            fileReader = new DataInputStream(new FileInputStream(file));
            System.out.println("客户端图片路径：" + path);

            String time = Utils.getTime();
            fileOut.writeUTF(time);
            fileOut.flush();

            // 封装文件名
            fileOut.writeUTF(file.getName());
            fileOut.flush();

            // 封装文件长度
            fileOut.writeLong(file.length());
            fileOut.flush();

            // 封装目的对象类型
            fileOut.writeUTF(currentChannelType);
            fileOut.flush();

            // 封装目的对象
            fileOut.writeUTF(currentObject);
            fileOut.flush();

            fileOut.writeUTF(userName);
            fileOut.flush();

            System.out.println("接收图片的目的对象：" + currentObject);

            int length = -1;
            byte[] buff = new byte[1024];
            while ((length = fileReader.read(buff)) > 0) {  // 发送内容
                System.out.println("正在上传图片...");
                fileOut.write(buff, 0, length);
                fileOut.flush();
            }
            System.out.print("上传图片完毕");
        } catch (Exception e) {
        }
    }

    // 客户端接收图片
    public void run() {
        try {
            socket = new Socket(Utils.ServerIP, Utils.ServerPicThreadPort);  // 客户端套接字
            System.out.println("创建客户端图片socket");
            fileIn = new DataInputStream(socket.getInputStream());  // 输入流
            fileOut = new DataOutputStream(socket.getOutputStream());  // 输出流

            //获取数据
            JSONObject data = new JSONObject();
            data.put("username", userName);
            data.put("msg", null);
//            System.out.println(data);
            fileOut.writeUTF(data.toString());

            // 接收图片
            while (true) {
                String time = fileIn.readUTF();
                String textName = fileIn.readUTF();
                long totleLength = fileIn.readLong();
                String currentChannelType = fileIn.readUTF();
                String currentObject = fileIn.readUTF();
                String sourceObject = fileIn.readUTF();

//                System.out.println(Client.message_area);

                int length = -1;
                byte[] buff = new byte[1024];
                long curLength = 0;

                File userFile = new File(Utils.Default_Download_Pictures_Path + "\\" + userName);
                if (!userFile.exists()) {  // 新建当前用户的文件夹
                    userFile.mkdirs();
                }
                File targetfile = new File(Utils.Default_Download_Pictures_Path + "\\" + userName + "\\" + textName);
                fileWriter = new DataOutputStream(Files.newOutputStream(targetfile.toPath()));
                while ((length = fileIn.read(buff)) > 0) {  // 把文件写进本地
                    fileWriter.write(buff, 0, length);
                    fileWriter.flush();
                    curLength += length;
                    if (curLength == totleLength) {  // 强制结束
                        break;
                    }
                }
                fileWriter.close();
                try { // 插入文本
                    JTextPane temp;
                    if(currentChannelType.equals("private")) {
                        Client.StartPrivateChat(currentObject);
                        temp = Client.messagePrivate.get(currentObject);
                    }
                    else {
                        Client.StartGroupChat(currentObject);
                        temp = Client.messageGroup.get(currentObject);
                    }
                    StyledDocument text = temp.getStyledDocument();


                    //切换窗口或收到新消息时,读取该对话框之前的聊天记录并添加新消息后呈现(只能重现文字内容)
                    //文字内容呈现现的工作逻辑:从HashMap中读取对应的JTextPane对象,先调用insertString方法在旧文本之后添加新文本,再调用getText方法获得文本内容,最后调用setText方法替换message_area中的文本内容
                    //图片内容在切换窗口或收到新信息后丢失:理想的图片内容更新逻辑与文字内容类似,但JTextPane类只提供了insertIcon方法,既未提供get方法获得图片内容,也未提供set方法替换图片内容
                    //为什么要必须使用" set + get "方法而不使用" = "进行赋值:我们希望修改message_area指向的内存地址的内容,而“ = ”是将message_area指向了另一块内存地址(而不是将原地址内容进行对应修改)
                    text.insertString(text.getLength(), sourceObject + " " + time + "\n", null);
                    Client.message_area.setText(temp.getText());

                    Client.message_area.setCaretPosition(text.getLength()); // 设置插入位置
                    ImageIcon image = new ImageIcon(targetfile.getPath());
                    image.setImage(image.getImage().getScaledInstance(-1, 150, Image.SCALE_DEFAULT));
                    Client.message_area.insertIcon(image); // 插入图片

                    text.insertString(text.getLength(), "[图片] " + textName + "\n", null);

                    if(currentChannelType.equals("private"))
                        Client.messagePrivate.replace(currentObject,temp);
                    else Client.messageGroup.replace(currentObject, temp);

                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
        }
    }

}

