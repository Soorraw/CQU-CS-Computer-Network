import net.sf.json.JSONObject;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;

public class ClientFileThread extends Thread {

    private Socket socket = null;
    static String userName = null;
    static PrintWriter out = null;  // 普通消息的发送（Server.java传来的值）
    static DataInputStream fileIn = null;
    static DataOutputStream fileOut = null;
    static DataInputStream fileReader = null;
    static DataOutputStream fileWriter = null;

    public ClientFileThread(String userName) {
        ClientFileThread.userName = userName;

    }

    // 客户端发送文件
    static void outFileToServer(String path, String currentChannelType, String currentObject) {
        try {
            System.out.println("客户端上传文件");
            File file = new File(path);
            fileReader = new DataInputStream(new FileInputStream(file));

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

            int length = -1;
            byte[] buff = new byte[1024];
            while ((length = fileReader.read(buff)) > 0) {  // 发送内容
                System.out.println("正在上传...");
                fileOut.write(buff, 0, length);
                fileOut.flush();
            }
            System.out.print("上传完毕");
//            out.println("【" + userName + "已成功发送文件！】");
//            out.flush();
        } catch (Exception e) {
        }
    }

    // 客户端接收文件
    public void run() {
        try {
//            InetAddress addr = InetAddress.getByName(null);  // 获取主机地址
            socket = new Socket(Utils.ServerIP, Utils.ServerFileThreadPort);  // 客户端套接字
            System.out.println("创建客户端文件socket");
            fileIn = new DataInputStream(socket.getInputStream());  // 输入流
            fileOut = new DataOutputStream(socket.getOutputStream());  // 输出流

            //获取数据
            JSONObject data = new JSONObject();
            data.put("username", userName);
            data.put("msg", null);
            fileOut.writeUTF(data.toString());

            // 接收文件
            while (true) {

                String time = fileIn.readUTF();
                String textName = fileIn.readUTF();
                long totleLength = fileIn.readLong();
                String currentChannelType = fileIn.readUTF();
                String currentObject = fileIn.readUTF();
                String sourceObject = fileIn.readUTF();
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

                    text.insertString(text.getLength(), sourceObject + "  " + time + "\n" + "[文件] " + textName + "\n", null);
                    //切换窗口或收到新消息时,读取该对话框之前的聊天记录并添加新消息后呈现(只能重现文字内容)
                    //文字内容呈现现的工作逻辑:从HashMap中读取对应的JTextPane对象,先调用insertString方法在旧文本之后添加新文本,再调用getText方法获得文本内容,最后调用setText方法替换message_area中的文本内容
                    //图片内容在切换窗口或收到新信息后丢失:理想的图片内容更新逻辑与文字内容类似,但JTextPane类只提供了insertIcon方法,既未提供get方法获得图片内容,也未提供set方法替换图片内容
                    //为什么要必须使用" set + get "方法而不使用" = "进行赋值:我们希望修改message_area指向的内存地址的内容,而“ = ”是将message_area指向了另一块内存地址(而不是将原地址内容进行对应修改)
                    Client.message_area.setText(temp.getText());

                    if(currentChannelType.equals("private"))
                        Client.messagePrivate.replace(currentObject,temp);
                    else Client.messageGroup.replace(currentObject, temp);

                } catch (BadLocationException e) {
                    e.printStackTrace();
                }

                if(sourceObject.equals(userName)){
                    continue;
                }

                int result = JOptionPane.showConfirmDialog(new JPanel(), "是否接受？", "提示",
                        JOptionPane.YES_NO_OPTION);
                int length = -1;
                byte[] buff = new byte[1024];
                long curLength = 0;
                int confirm = -1;
                // 提示框选择结果，0为确定，1位取消
                if (result == 0) {
//					out.println("【" + userName + "选择了接收文件！】");
//					out.flush();
                    JFileChooser fileChooser = new JFileChooser(Utils.Default_Download_Path);
                    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    confirm = fileChooser.showDialog(new JLabel(), "选择");

                    if(confirm == JFileChooser.APPROVE_OPTION){
                        File selectedfile = fileChooser.getSelectedFile();

                        File targetfile = new File(selectedfile.getAbsolutePath() + "\\" + textName);
//                    System.out.println(targetfile.toPath());
                        fileWriter = new DataOutputStream(Files.newOutputStream(targetfile.toPath()));
                        while ((length = fileIn.read(buff)) > 0) {  // 把文件写进本地
                            fileWriter.write(buff, 0, length);
                            fileWriter.flush();
                            curLength += length;
//						out.println("【接收进度:" + curLength/totleLength*100 + "%】");
//						out.flush();
                            if (curLength == totleLength) {  // 强制结束
                                break;
                            }
                        }
                        // 提示文件存放地址
                        JOptionPane.showMessageDialog(new JPanel(), "文件存放地址：\n" +
                                selectedfile.getAbsolutePath() + "\\" + textName, "提示", JOptionPane.INFORMATION_MESSAGE);
                        fileWriter.close();
                    }
                }
                if(result == 1 || confirm != JFileChooser.APPROVE_OPTION)
                {  // 不接受文件
                    while ((length = fileIn.read(buff)) > 0) {
                        curLength += length;
                        if (curLength == totleLength) {  // 强制结束
                            break;
                        }
                    }
                }

            }
        } catch (Exception e) {
        }
    }

}

